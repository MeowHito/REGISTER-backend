package com.actionth.membership.service;

import com.actionth.membership.config.MPayConfig;
import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.OrderUpdateResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import org.redisson.api.RList;
import org.redisson.api.RMap;
import org.redisson.api.RedissonClient;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * mPAY (AIS) gateway — card, PromptPay QR, Internet/Mobile Banking, Rabbit LINE Pay, ShopeePay
 * and Paotang through one API ("mPAY ONE API Specification" v4.19).
 *
 * <p>mPAY's {@code order_id} must be unique per merchant, but a runner can come back and pay the
 * same order again (another method, a fresh QR). Each attempt therefore gets its own id —
 * {@code orderNo} + 6 random characters, exactly the 20 characters mPAY allows — while
 * {@code ref_1} always carries our {@code orderNo}. Attempt ids are remembered in Redis so an
 * order can be enquired when the runner returns before the webhook.</p>
 *
 * <p>Money: Action charges its own fee (constant/PaymentFee), so {@code amount} is the order's
 * {@code totalAmountWithFee}; mPAY's customer fee must be configured to 0 on their side.</p>
 */
@Service
@Slf4j
public class MPayPaymentService {

    /** Payment methods mPAY serves, in the order the payment page lists them. */
    public static final List<String> METHODS = List.of(
            "qrcode", "creditcard", "mobilebanking", "linepay", "shopeepay", "paotang");

    private static final Set<String> REDIRECT_METHODS = Set.of(
            "creditcard", "mobilebanking", "linepay", "shopeepay", "paotang");

    private static final Map<String, String> BANK_NAMES = Map.of(
            "002", "BBL", "004", "KBANK", "006", "KTB", "011", "TTB",
            "014", "SCB", "025", "BAY", "088", "CLICX");

    private static final String ATTEMPTS_PREFIX = "mpay:attempts:";
    private static final Duration ATTEMPTS_TTL = Duration.ofDays(5);
    private static final int MAX_ATTEMPTS_KEPT = 10;
    private static final int MAX_ENQUIRY_ATTEMPTS = 3;
    private static final String QR_CACHE_PREFIX = "qr:mpay:";
    private static final char[] ALNUM = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private final MPayConfig config;
    private final ObjectMapper objectMapper;
    private final RedissonClient redissonClient;
    private final AppErrorLogService appErrorLogService;
    private final PaymentWebhookService webhookService;
    private final RestTemplate restTemplate;

    public MPayPaymentService(MPayConfig config,
                              ObjectMapper objectMapper,
                              RedissonClient redissonClient,
                              AppErrorLogService appErrorLogService,
                              PaymentWebhookService webhookService) {
        this.config = config;
        this.objectMapper = objectMapper;
        this.redissonClient = redissonClient;
        this.appErrorLogService = appErrorLogService;
        this.webhookService = webhookService;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10_000);
        factory.setReadTimeout(30_000);
        this.restTemplate = new RestTemplate(factory);
    }

    // ──────────────────────────────────────────────
    // What the payment page may offer
    // ──────────────────────────────────────────────

    public boolean isActive() {
        return config.isActive();
    }

    /** Methods with a configured service id. */
    public List<String> availableMethods() {
        return METHODS.stream().filter(m -> config.serviceIdFor(m) != null).toList();
    }

    public List<Map<String, String>> availableBanks() {
        List<Map<String, String>> banks = new ArrayList<>();
        for (String code : config.bankCodes()) {
            banks.add(Map.of("code", code, "name", BANK_NAMES.getOrDefault(code, code)));
        }
        return banks;
    }

    public boolean supports(String paymentMethod) {
        return paymentMethod != null && availableMethods().contains(paymentMethod);
    }

    // ──────────────────────────────────────────────
    // Start a payment
    // ──────────────────────────────────────────────

    /**
     * @param bankCode     Internet/Mobile Banking bank (spec §11), required for "mobilebanking"
     * @param mobileClient whether the runner is on a phone (ShopeePay platform_type, Paotang/LINE app link)
     */
    public OrderUpdateResponse.PaymentData initiatePayment(Orders order, String bankCode, boolean mobileClient) {
        String method = order.getPaymentMethod();
        String serviceId = config.serviceIdFor(method);
        if (!isActive() || serviceId == null) {
            throw new BusinessException("ช่องทางชำระเงินนี้ยังไม่เปิดให้บริการ");
        }

        BigDecimal amount = BigDecimal.valueOf(order.getTotalAmountWithFee()).setScale(2, RoundingMode.HALF_UP);

        if ("qrcode".equals(method)) {
            return initiateQr(order, serviceId, amount);
        }
        if (!REDIRECT_METHODS.contains(method)) {
            throw new BusinessException("ช่องทางชำระเงินนี้ยังไม่เปิดให้บริการ");
        }

        String attemptId = newAttemptId(order.getOrderNo());
        Map<String, Object> body = baseBody(order, attemptId, serviceId, amount);
        String path;
        String returnSuccess = returnUrl(order.getOrderNo(), "success");
        String returnFail = returnUrl(order.getOrderNo(), "fail");

        switch (method) {
            case "creditcard" -> {
                path = "/service-txn-gateway/v1/cc/txns/payment_order";
                body.put("form_type", "FORM");
                body.put("is_remember", false);
                body.put("capture", true);
                Map<String, Object> threeDs = new LinkedHashMap<>();
                threeDs.put("3ds_required", true);
                threeDs.put("3ds_url_success", returnSuccess);
                threeDs.put("3ds_url_fail", returnFail);
                body.put("3ds", threeDs);
            }
            case "mobilebanking" -> {
                if (bankCode == null || !config.bankCodes().contains(bankCode)) {
                    throw new BusinessException("กรุณาเลือกธนาคารสำหรับชำระผ่าน Mobile Banking");
                }
                path = "/service-txn-gateway/v1/ib/payment_order";
                body.put("bank_code", bankCode);
                body.put("redirect_urls", Map.of("url_success", returnSuccess, "url_fail", returnFail));
            }
            case "linepay" -> {
                path = "/service-txn-gateway/v1/rlp/txns/payment";
                body.put("is_pre_approve", false);
                Map<String, Object> product = new LinkedHashMap<>();
                product.put("id", order.getOrderNo());
                product.put("name", productName(order));
                product.put("quantity", 1);
                product.put("price", amount);
                Map<String, Object> pkg = new LinkedHashMap<>();
                pkg.put("id", attemptId);
                pkg.put("amount", amount);
                pkg.put("products", List.of(product));
                body.put("packages", List.of(pkg));
                body.put("redirect_urls", Map.of("confirm_url", returnSuccess, "cancel_url", returnFail));
            }
            case "shopeepay" -> {
                path = "/service-txn-gateway/v1/shopee-pay/payment_order";
                body.put("redirect_urls", Map.of("url_success", returnSuccess, "url_fail", returnFail));
                body.put("platform_type", mobileClient ? "mweb" : "pc");
            }
            case "paotang" -> {
                path = "/service-txn-gateway/v1/paotang/payment_order";
                body.put("redirect_urls", Map.of("url_success", returnSuccess, "url_fail", returnFail));
            }
            default -> throw new BusinessException("ช่องทางชำระเงินนี้ยังไม่เปิดให้บริการ");
        }

        rememberAttempt(order.getOrderNo(), attemptId);
        Map<String, Object> response = post(path, body, order.getOrderNo());

        // Card/banking/ShopeePay answer at the top level; Rabbit LINE Pay nests it under "data".
        Map<String, Object> data = asMap(response.get("data"));
        String webUrl = firstNonBlank(
                str(response.get("form_url")),
                str(response.get("authorize_url")),
                str(response.get("web_redirect")),
                data != null ? str(data.get("web_redirect")) : null);
        String appUrl = firstNonBlank(
                str(response.get("app_redirect")),
                data != null ? str(data.get("app_redirect")) : null);

        if (isBlank(webUrl) && isBlank(appUrl)) {
            log.error("[mPAY] No redirect URL: orderNo={}, method={}, response={}", order.getOrderNo(), method, response);
            appErrorLogService.logBackendError("error", "MPAY_PAYMENT",
                    "No redirect URL: orderNo=" + order.getOrderNo() + ", method=" + method + ", response=" + response, null);
            throw new BusinessException("ไม่สามารถเริ่มการชำระเงินได้ กรุณาลองใหม่หรือเลือกช่องทางอื่น");
        }

        return OrderUpdateResponse.PaymentData.builder()
                .provider("MPAY")
                .paymentMethod(method)
                .webPaymentUrl(firstNonBlank(webUrl, appUrl))
                .appPaymentUrl(appUrl)
                .refNo(order.getOrderNo())
                .build();
    }

    private OrderUpdateResponse.PaymentData initiateQr(Orders order, String serviceId, BigDecimal amount) {
        String orderNo = order.getOrderNo();
        String amountKey = amount.toPlainString();
        RMap<String, String> cache = redissonClient.getMap(QR_CACHE_PREFIX + orderNo);
        try {
            String cachedQr = cache.get("qrImage");
            if (!isBlank(cachedQr) && amountKey.equals(cache.get("amount"))) {
                log.info("[mPAY QR] Reusing cached QR for orderNo={}, amount={}", orderNo, amountKey);
                return qrData(orderNo, cachedQr, cache.get("expireAt"));
            }
        } catch (Exception e) {
            log.warn("[mPAY QR] Failed to read cached QR for orderNo={}, regenerating", orderNo, e);
        }

        long expireSeconds = qrExpireSeconds(order);
        if (expireSeconds <= 0) {
            throw new BusinessException("เลยกำหนดชำระเงินแล้ว");
        }

        String attemptId = newAttemptId(orderNo);
        Map<String, Object> body = baseBody(order, attemptId, serviceId, amount);
        body.remove("channel_type");
        body.put("sof", "PROMPTPAY");
        body.put("expire_time_seconds", expireSeconds);

        rememberAttempt(orderNo, attemptId);
        Map<String, Object> response = post("/service-txn-gateway/v1/qr", body, orderNo);

        String qrImage = str(response.get("qr_img"));
        if (isBlank(qrImage)) {
            log.error("[mPAY QR] No qr_img: orderNo={}, response={}", orderNo, response);
            appErrorLogService.logBackendError("error", "MPAY_QR",
                    "No qr_img: orderNo=" + orderNo + ", response=" + response, null);
            throw new BusinessException("ไม่สามารถสร้าง QR Code ได้ กรุณาลองใหม่");
        }
        String expireAt = OffsetDateTime.now().plusSeconds(expireSeconds).toString();
        try {
            cache.clear();
            cache.put("qrImage", qrImage);
            cache.put("amount", amountKey);
            cache.put("expireAt", expireAt);
            // Drop the cache a little early so a runner never gets a QR that dies mid-scan.
            cache.expire(Duration.ofSeconds(Math.max(1, expireSeconds - 30)));
        } catch (Exception e) {
            log.warn("[mPAY QR] Failed to cache QR for orderNo={}", orderNo, e);
        }
        return qrData(orderNo, qrImage, expireAt);
    }

    private OrderUpdateResponse.PaymentData qrData(String orderNo, String qrImage, String expireAt) {
        return OrderUpdateResponse.PaymentData.builder()
                .provider("MPAY")
                .paymentMethod("qrcode")
                .qrImage(stripDataUrlPrefix(qrImage))
                .qrExpireAt(expireAt)
                .refNo(orderNo)
                .build();
    }

    private long qrExpireSeconds(Orders order) {
        long max = Math.max(60, config.getQrExpireSeconds());
        OffsetDateTime due = order.getPaymentDueDatetime();
        if (due == null) {
            return max;
        }
        long untilDue = Duration.between(OffsetDateTime.now(), due).getSeconds();
        return Math.min(max, untilDue);
    }

    // ──────────────────────────────────────────────
    // Enquiry — when the runner comes back before (or without) the webhook
    // ──────────────────────────────────────────────

    /**
     * Asks mPAY about this order's latest attempts and settles the first successful one through
     * the same checks as the webhook. Returns {settled, status}.
     */
    public Map<String, Object> enquireAndSettle(String orderNo) {
        if (!isActive() || isBlank(orderNo)) {
            return Map.of("settled", false, "status", "PENDING");
        }
        List<String> attempts = attempts(orderNo);
        for (String attemptId : attempts.subList(0, Math.min(MAX_ENQUIRY_ATTEMPTS, attempts.size()))) {
            Map<String, Object> response;
            try {
                response = post("/service-txn-gateway/v1/enquiry", Map.of("order_id", attemptId), orderNo);
            } catch (Exception e) {
                log.warn("[mPAY enquiry] attempt={} failed: {}", attemptId, e.getMessage());
                continue;
            }
            String status = upper(str(response.get("status")));
            String txnType = upper(str(response.get("txn_type")));
            if ("SUCCESS".equals(status) && (txnType == null || "PAYMENT".equals(txnType))) {
                Map<String, Object> result = webhookService.processMPayResult(response, toJson(response), "ENQUIRY");
                String settledStatus = str(result.get("status"));
                return Map.of(
                        "settled", "SUCCESS".equals(settledStatus),
                        "status", settledStatus != null ? settledStatus : "PENDING");
            }
        }
        return Map.of("settled", false, "status", "PENDING");
    }

    public boolean hasAttempts(String orderNo) {
        return !isBlank(orderNo) && !attempts(orderNo).isEmpty();
    }

    // ──────────────────────────────────────────────
    // Signature — HEX(HMAC-SHA256(ChannelSecret, body + nonce))
    // ──────────────────────────────────────────────

    public String sign(String body, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(config.getChannelSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal((body + nonce).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(config.isSignatureUppercase() ? "%02X" : "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign mPAY request", e);
        }
    }

    /** Webhook check: same formula over the raw body exactly as received. */
    public boolean verifySignature(String rawBody, String nonce, String signature) {
        if (isBlank(config.getChannelSecret()) || isBlank(nonce) || isBlank(signature) || rawBody == null) {
            return false;
        }
        String expected = sign(rawBody, nonce).toLowerCase();
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                signature.trim().toLowerCase().getBytes(StandardCharsets.US_ASCII));
    }

    // ──────────────────────────────────────────────
    // HTTP
    // ──────────────────────────────────────────────

    private Map<String, Object> post(String path, Map<String, Object> body, String orderNo) {
        String url = trimTrailingSlash(config.getBaseUrl()) + path;
        String json = toJson(body);
        String nonce = UUID.randomUUID().toString();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
        headers.set("X-sdpg-nonce", nonce);
        headers.set("X-sdpg-merchant-id", config.getMerchantId());
        headers.set("X-sdpg-signature", sign(json, nonce));

        try {
            // Send the exact string that was signed.
            ResponseEntity<String> res = restTemplate.postForEntity(url, new HttpEntity<>(json, headers), String.class);
            Map<String, Object> parsed = parse(res.getBody());
            log.info("[mPAY] {} orderNo={} -> {} status={}", path, orderNo, res.getStatusCodeValue(), parsed.get("status"));
            return parsed;
        } catch (HttpStatusCodeException e) {
            Map<String, Object> err = parse(e.getResponseBodyAsString());
            String errorCode = firstNonBlank(str(err.get("errorCode")), str(err.get("error_code")), e.getStatusCode().toString());
            log.error("[mPAY] {} orderNo={} failed: http={}, errorCode={}, body={}",
                    path, orderNo, e.getRawStatusCode(), errorCode, e.getResponseBodyAsString());
            appErrorLogService.logBackendError("error", "MPAY_API",
                    path + " orderNo=" + orderNo + " http=" + e.getRawStatusCode() + " errorCode=" + errorCode
                            + " body=" + e.getResponseBodyAsString(), null);
            throw new BusinessException("ระบบชำระเงินขัดข้อง (" + errorCode + ") กรุณาลองใหม่หรือเลือกช่องทางอื่น");
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[mPAY] {} orderNo={} error", path, orderNo, e);
            appErrorLogService.logBackendError("error", "MPAY_API",
                    path + " orderNo=" + orderNo + ": " + e.getMessage(), Arrays.toString(e.getStackTrace()));
            throw new BusinessException("ไม่สามารถเชื่อมต่อระบบชำระเงินได้ กรุณาลองใหม่อีกครั้ง");
        }
    }

    private Map<String, Object> baseBody(Orders order, String attemptId, String serviceId, BigDecimal amount) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("order_id", attemptId);
        body.put("product_name", productName(order));
        body.put("service_id", serviceId);
        body.put("channel_type", "WEBSITE");
        body.put("cust_id", custId(order));
        body.put("amount", amount);
        body.put("currency", "THB");
        body.put("ref_1", order.getOrderNo());
        if (order.getEvent() != null && order.getEvent().getUuid() != null) {
            body.put("ref_2", order.getEvent().getUuid());
        }
        return body;
    }

    private String productName(Orders order) {
        String eventName = order.getEvent() != null ? order.getEvent().getName() : null;
        String name = "ค่าสมัคร " + (isBlank(eventName) ? order.getOrderNo() : eventName.trim());
        return name.length() > 200 ? name.substring(0, 200) : name;
    }

    private String custId(Orders order) {
        if (order.getCreatedBy() != null && !isBlank(order.getCreatedBy().getUuid())) {
            return order.getCreatedBy().getUuid();
        }
        return order.getOrderNo();
    }

    private String returnUrl(String orderNo, String result) {
        return trimTrailingSlash(config.getBeUrl()) + "/public-api/mpay/return?orderNo="
                + URLEncoder.encode(orderNo, StandardCharsets.UTF_8) + "&result=" + result;
    }

    // ──────────────────────────────────────────────
    // Attempt ids
    // ──────────────────────────────────────────────

    private String newAttemptId(String orderNo) {
        StringBuilder sb = new StringBuilder(orderNo);
        while (sb.length() < 20) {
            sb.append(ALNUM[RANDOM.nextInt(ALNUM.length)]);
        }
        return sb.substring(0, 20);
    }

    private void rememberAttempt(String orderNo, String attemptId) {
        try {
            RList<String> list = redissonClient.getList(ATTEMPTS_PREFIX + orderNo);
            list.add(0, attemptId);
            if (list.size() > MAX_ATTEMPTS_KEPT) {
                list.trim(0, MAX_ATTEMPTS_KEPT - 1);
            }
            list.expire(ATTEMPTS_TTL);
        } catch (Exception e) {
            // The webhook still settles by ref_1; only the early enquiry is lost.
            log.warn("[mPAY] Failed to remember attempt {} for orderNo={}", attemptId, orderNo, e);
        }
    }

    private List<String> attempts(String orderNo) {
        try {
            RList<String> list = redissonClient.getList(ATTEMPTS_PREFIX + orderNo);
            return new ArrayList<>(list.readAll());
        } catch (Exception e) {
            log.warn("[mPAY] Failed to read attempts for orderNo={}", orderNo, e);
            return List.of();
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    public static boolean isMobileUserAgent(String userAgent) {
        if (userAgent == null) {
            return false;
        }
        String ua = userAgent.toLowerCase();
        return ua.contains("mobi") || ua.contains("android") || ua.contains("iphone") || ua.contains("ipad");
    }

    private String toJson(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot serialise mPAY body", e);
        }
    }

    private Map<String, Object> parse(String json) {
        if (isBlank(json)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (Exception e) {
            Map<String, Object> raw = new LinkedHashMap<>();
            raw.put("raw", json);
            return raw;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o) {
        return o instanceof Map<?, ?> m ? (Map<String, Object>) m : null;
    }

    private static String stripDataUrlPrefix(String img) {
        if (img == null) {
            return null;
        }
        int comma = img.indexOf(',');
        return img.startsWith("data:") && comma > 0 ? img.substring(comma + 1) : img;
    }

    private static String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        String t = s.trim();
        return t.endsWith("/") ? t.substring(0, t.length() - 1) : t;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String upper(String s) {
        return s == null ? null : s.trim().toUpperCase();
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) {
            if (!isBlank(v)) {
                return v;
            }
        }
        return null;
    }
}
