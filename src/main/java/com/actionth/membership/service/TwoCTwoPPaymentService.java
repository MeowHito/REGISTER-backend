package com.actionth.membership.service;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.model.Orders;
import com.actionth.membership.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.actionth.membership.config.PaymentConfig;
import com.actionth.membership.exception.BusinessException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 2C2P Payment Service — handles Credit/Debit Card, E-wallet (LINE Pay, TrueMoney).
 */
@Service
@Slf4j
public class TwoCTwoPPaymentService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final PaymentConfig paymentConfig;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentWebhookService webhookService;
    private final ObjectMapper objectMapper;
    private final AppErrorLogService appErrorLogService;

    public TwoCTwoPPaymentService(PaymentConfig paymentConfig,
                                  OrderService orderService,
                                  OrderRepository orderRepository,
                                  PaymentWebhookService webhookService,
                                  ObjectMapper objectMapper,
                                  AppErrorLogService appErrorLogService) {
        this.paymentConfig = paymentConfig;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.webhookService = webhookService;
        this.objectMapper = objectMapper;
        this.appErrorLogService = appErrorLogService;
    }

    // ──────────────────────────────────────────────
    // 2C2P Payment Token (Credit Card, E-wallet)
    // ──────────────────────────────────────────────

    /**
     * Creates a 2C2P payment token and returns the webPaymentUrl and payload.
     *
     * @param invoiceNo       the order/invoice number
     * @param amount          formatted amount (e.g. "1500.00")
     * @param paymentChannels list of channels, e.g. ["CC"] or ["LINE","TRUEMONEY"]
     * @return map with "webPaymentUrl" and "payload" keys
     */
    public Map<String, Object> createPaymentToken(String invoiceNo, String amount, String[] paymentChannels) {
        try {
            boolean isCreditCard = paymentChannels.length > 0 && "CC".equals(paymentChannels[0]);
            String merchantId = isCreditCard
                    ? paymentConfig.getMerchantId2C2P()
                    : paymentConfig.getMerchantIdEwallet2C2P();
            String secretKey = paymentConfig.getSecretKey2C2P();
            String tokenUrl = paymentConfig.getTokenUrl2C2P();

            String nonceStr = LocalDateTime.now()
                    .format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

            Map<String, Object> payloadMap = new LinkedHashMap<>();
            payloadMap.put("merchantID", merchantId);
            payloadMap.put("invoiceNo", invoiceNo);
            payloadMap.put("description", "ค่าสมัครการแข่งขัน");
            payloadMap.put("amount", amount);
            payloadMap.put("currencyCode", "THB");
            payloadMap.put("nonceStr", nonceStr);
            payloadMap.put("paymentChannel", paymentChannels);
            payloadMap.put("frontendReturnUrl", paymentConfig.getFrontendReturnUrl2C2P());
            payloadMap.put("backendReturnUrl", paymentConfig.getBackendReturnUrl2C2P());

            String jwt = signJwt(payloadMap, secretKey);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "text/plain");

            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("payload", jwt);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(reqBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(tokenUrl, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return processTokenResponse(response.getBody());
            }

            log.error("[2C2P Token] Request failed: invoiceNo={}, status={}", invoiceNo, response.getStatusCode());
            appErrorLogService.logBackendError("error", "2C2P_TOKEN",
                    "Token request failed: invoiceNo=" + invoiceNo + ", status=" + response.getStatusCode(), null);
            throw new BusinessException("2C2P Token Request Failed: " + response.getStatusCode());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("[2C2P Token] Error creating payment token, invoiceNo={}", invoiceNo, e);
            appErrorLogService.logBackendError("error", "2C2P_TOKEN",
                    "Exception creating payment token: invoiceNo=" + invoiceNo + ": " + e.getMessage(),
                    Arrays.toString(e.getStackTrace()));
            throw new BusinessException("Failed to create 2C2P payment token: " + e.getMessage());
        }
    }

    private Map<String, Object> processTokenResponse(String responseBody) throws Exception {
        Map<String, Object> respJson = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {});
        String respJwt = safeString(respJson.get("payload"));

        if (respJwt == null || respJwt.isBlank()) {
            log.error("[2C2P Token] Response missing 'payload' field. Raw response: {}", responseBody);
            throw new BusinessException("2C2P returned empty payload. Response: " + responseBody);
        }

        Map<String, Object> decoded = PaymentWebhookService.decodeJwtNoVerify(respJwt);

        String respCode = safeString(decoded.get("respCode"));
        String webPaymentUrl = safeString(decoded.get("webPaymentUrl"));
        String respDesc = safeString(decoded.get("respDesc"));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("payload", respJwt);
        result.put("respCode", respCode);
        result.put("respDesc", respDesc);
        result.put("webPaymentUrl", webPaymentUrl);
        return result;
    }

    private String signJwt(Map<String, Object> payload, String secretKey) throws Exception {
        Map<String, String> header = new HashMap<>();
        header.put("alg", "HS256");
        header.put("typ", "JWT");

        String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
        String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));
        String data = encodedHeader + "." + encodedPayload;

        Mac sha256HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec key = new SecretKeySpec(
                secretKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256HMAC.init(key);

        byte[] signatureBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

        return data + "." + signature;
    }

    // ──────────────────────────────────────────────
    // 2C2P Payment Inquiry
    // ──────────────────────────────────────────────

    /**
     * @param invoiceNo   invoice number to inquire
     * @param paymentType "CREDIT_CARD" or "LINE_TRUEMONEY" — determines which merchant ID to use
     */
    public Map<String, Object> inquirePayment(String invoiceNo, String paymentType) {
        try {
            String merchantId = resolveMerchantId(paymentType);
            if (merchantId == null || paymentConfig.getSecretKey2C2P() == null
                    || paymentConfig.getInquiryUrl2C2P() == null) {
                throw new BusinessException("Missing 2C2P Config");
            }

            Map<String, String> header = new HashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, String> payload = new HashMap<>();
            payload.put("merchantID", merchantId);
            payload.put("invoiceNo", invoiceNo);

            String encodedHeader = base64UrlEncode(objectMapper.writeValueAsString(header));
            String encodedPayload = base64UrlEncode(objectMapper.writeValueAsString(payload));
            String data = encodedHeader + "." + encodedPayload;

            Mac sha256HMAC = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    paymentConfig.getSecretKey2C2P().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256");
            sha256HMAC.init(secretKey);

            byte[] signatureBytes = sha256HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
            String signature = Base64.getUrlEncoder().withoutPadding().encodeToString(signatureBytes);

            String jwt = data + "." + signature;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Accept", "text/plain");

            Map<String, String> reqBody = new HashMap<>();
            reqBody.put("payload", jwt);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(reqBody, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    paymentConfig.getInquiryUrl2C2P(), entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return processInquiryResponse(response.getBody(), invoiceNo);
            }

            throw new BusinessException("2C2P Inquiry Failed: " + response.getStatusCode());

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("2C2P Inquiry Error", e);
            Map<String, Object> err = new HashMap<>();
            err.put("error", e.getMessage());
            return err;
        }
    }

    private Map<String, Object> processInquiryResponse(String responseBody, String invoiceNo) throws Exception {
        Map<String, Object> respJson = objectMapper.readValue(responseBody,
                new TypeReference<Map<String, Object>>() {
                });
        String respJwt = safeString(respJson.get("payload"));

        if (respJwt == null || respJwt.isBlank()) {
            log.error("[2C2P Inquiry] Response missing 'payload' field. invoiceNo={}, raw response: {}", invoiceNo, responseBody);
            throw new BusinessException("2C2P inquiry returned empty payload for invoiceNo=" + invoiceNo);
        }

        Map<String, Object> decoded = PaymentWebhookService.decodeJwtNoVerify(respJwt);

        String respCode = safeString(decoded.get("respCode"));
        String respDesc = safeString(decoded.get("respDesc"));
        String tranRef = safeString(decoded.get("tranRef"));

        String code = (respCode == null) ? "" : respCode.trim();
        boolean isSuccess = "0000".equals(code);
        boolean isPending = "0001".equals(code) || "2001".equals(code);

        String status;
        boolean settled;

        if (isSuccess) {
            status = "SUCCESS";
            settled = true;
            settleOrder(invoiceNo, tranRef);
        } else if (isPending) {
            status = "PENDING";
            settled = false;
        } else {
            status = "FAILED";
            settled = false;
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("settled", settled);
        out.put("status", status);
        out.put("invoiceNo", invoiceNo);
        out.put("respCode", respCode);
        out.put("respDesc", respDesc);
        return out;
    }

    private void settleOrder(String invoiceNo, String tranRef) {
        Orders order = orderRepository.findByUuidOrOrderNo(invoiceNo).orElse(null);
        if (order != null && !PaymentStatus.SUCCESS.toString().equalsIgnoreCase(order.getPaymentStatus())) {
            String txnId = (tranRef == null || tranRef.isBlank()) ? invoiceNo : tranRef;

            orderService.updatePaymentStatus(order.getOrderNo(), txnId, PaymentStatus.SUCCESS);
            if (order.getPaymentDateTime() == null) {
                order.setPaymentDateTime(OffsetDateTime.now());
            }
            webhookService.sendSuccessEmail(order, txnId);
        }
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String base64UrlEncode(String str) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(str.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new BusinessException(e.getMessage());
        }
    }

    /**
     * Picks the correct 2C2P merchant ID based on the payment type.
     * Credit/Debit Card → merchant-id, LINE Pay/TrueMoney → merchant-id-ewallet.
     */
    private String resolveMerchantId(String paymentType) {
        if ("LINE_TRUEMONEY".equalsIgnoreCase(paymentType)) {
            return paymentConfig.getMerchantIdEwallet2C2P();
        }
        return paymentConfig.getMerchantId2C2P(); // CREDIT_CARD or default
    }

    private String safeString(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}
