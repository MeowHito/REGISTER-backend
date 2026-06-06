package com.actionth.membership.service;

import com.actionth.membership.constant.PaymentStatus;
import com.actionth.membership.model.Orders;
import com.actionth.membership.repository.OrderRepository;

import lombok.extern.slf4j.Slf4j;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.actionth.membership.config.PaymentConfig;
import com.actionth.membership.exception.BusinessException;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * SCB Payment Service — handles QR Code, Slip Verify, Alipay, WeChatPay
 * (all routed through the SCB Open API gateway).
 */
@Service
@Slf4j
public class SCBPaymentService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final PaymentConfig paymentConfig;
    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final PaymentWebhookService webhookService;
    private final AppErrorLogService appErrorLogService;

    public SCBPaymentService(PaymentConfig paymentConfig,
            OrderService orderService,
            OrderRepository orderRepository,
            PaymentWebhookService webhookService,
            AppErrorLogService appErrorLogService) {
        this.paymentConfig = paymentConfig;
        this.orderService = orderService;
        this.orderRepository = orderRepository;
        this.webhookService = webhookService;
        this.appErrorLogService = appErrorLogService;
    }

    // ──────────────────────────────────────────────
    // SCB OAuth Token
    // ──────────────────────────────────────────────

    public String getAccessToken() {
        String url = paymentConfig.getBaseUrl() + "/v1/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("resourceOwnerId", paymentConfig.getAppKey());
        headers.set("requestUId", UUID.randomUUID().toString());
        headers.set("accept-language", "EN");

        Map<String, String> body = new HashMap<>();
        body.put("applicationKey", paymentConfig.getAppKey());
        body.put("applicationSecret", paymentConfig.getAppSecret());

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(body, headers);
        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity,
                new ParameterizedTypeReference<Map<String, Object>>() {
                });
        Map<String, Object> responseBody = response.getBody();

        if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
            Map<?, ?> data = asMap(responseBody.get("data"));
            if (data != null) {
                return (String) data.get("accessToken");
            }
        }
        log.error("[SCB OAuth] Failed to get access token, status={}, body={}", response.getStatusCode(), responseBody);
        appErrorLogService.logBackendError("error", "SCB_OAUTH",
                "Failed to get access token, status=" + response.getStatusCode(), null);
        throw new BusinessException("Failed to get access token from SCB API");
    }

    // ──────────────────────────────────────────────
    // QR Code (Thai QR / PromptPay)
    // ──────────────────────────────────────────────

    private static final ZoneId BKK = ZoneId.of("Asia/Bangkok");
    private static final DateTimeFormatter SCB_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final long QR_EXPIRE_MINUTES = 10L;

    public Map<String, Object> createQRCode(Map<String, Object> request, Orders order) {
        try {
            String expiryDateStr = resolveExpiryDateFromOrder(order);

            Map<String, Object> scbReq = new LinkedHashMap<>(request);
            scbReq.put("expiryDate", expiryDateStr);
            scbReq.put("numberOfTimes", "1");

            String accessToken = getAccessToken();
            String url = paymentConfig.getBaseUrl() + "/v1/payment/qrcode/create";

            HttpHeaders headers = scbHeaders(accessToken);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(scbReq, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                return responseBody;
            } else {
                log.error("[SCB QR] Failed to create QR Code, status={}, body={}", response.getStatusCode(), responseBody);
                appErrorLogService.logBackendError("error", "SCB_QR",
                        "Failed to create QR Code, status=" + response.getStatusCode() + ", ref1=" + request.get("ref1"), null);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "Failed to create QR Code");
                return errorResponse;
            }
        } catch (BusinessException ex) {
            log.warn("[SCB QR] Order is overdue, ref1={}", request.get("ref1"), ex);
            throw ex;
        } catch (Exception ex) {
            log.error("[SCB QR] Error calling SCB QR Code API, ref1={}", request.get("ref1"), ex);
            appErrorLogService.logBackendError("error", "SCB_QR",
                    "Exception calling SCB QR Code API, ref1=" + request.get("ref1") + ": " + ex.getMessage(),
                    Arrays.toString(ex.getStackTrace()));
            throw ex;
        }
    }

    private String resolveExpiryDateFromOrder(Orders order) {
        OffsetDateTime due = order.getPaymentDueDatetime();
        OffsetDateTime now = OffsetDateTime.now(BKK);
        OffsetDateTime dueInBkk = due.atZoneSameInstant(BKK).toOffsetDateTime();
        if (!dueInBkk.isAfter(now)) {
            throw new BusinessException("ORDER_OVERDUE");
        }

        OffsetDateTime maxQrExpiry = now.plusMinutes(QR_EXPIRE_MINUTES);
        OffsetDateTime effectiveExpiry = dueInBkk.isBefore(maxQrExpiry) ? dueInBkk : maxQrExpiry;

        return effectiveExpiry.toLocalDateTime().format(SCB_FMT);
    }

    // ──────────────────────────────────────────────
    // Slip Verify (Bill Payment Transaction Inquiry)
    // ──────────────────────────────────────────────

    private static final String DEFAULT_SENDING_BANK = "014";

    public Map<String, Object> verifyTransaction(String slipQrRawData, String expectedOrderNo) {
        MiniQrInfo miniQr = extractMiniQr(slipQrRawData);

        if (!miniQr.hasTransRef()) {
            return pendingReason("MISSING_TRANS_REF");
        }

        String transRef = miniQr.transRef();
        String resolvedSendingBank = resolveSendingBank(miniQr.sendingBank());

        String url = paymentConfig.getBaseUrl() + "/v1/payment/billpayment/transactions/" + transRef
                + "?sendingBank=" + resolvedSendingBank;

        String accessToken = getAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + accessToken);
        headers.set("requestUId", UUID.randomUUID().toString());
        headers.set("resourceOwnerId", paymentConfig.getAppKey());
        headers.set("accept-language", "EN");

        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return pending();
            }

            return processVerifyResponse(response.getBody(), transRef, expectedOrderNo);

        } catch (Exception ex) {
            log.error("[SLIP verify] downstream error transRef={}", transRef, ex);
            return pending();
        }
    }

    private Map<String, Object> processVerifyResponse(Map<String, Object> body,
            String transRef,
            String expectedOrderNo) {
        Map<?, ?> status = asMap(body.get("status"));
        Map<?, ?> data = asMap(body.get("data"));

        if (status == null || data == null)
            return pending();
        if (!"1000".equals(getString(status, "code")))
            return pending();

        String slipOrderNo = trimToNull(getString(data, "ref1"));
        if (slipOrderNo == null)
            return pending();

        String expected = trimToNull(expectedOrderNo);
        if (expected == null)
            return pending();

        if (!expected.equalsIgnoreCase(slipOrderNo)) {
            log.warn("[SLIP verify] slip not match order. expected={} ref1={}", expected, slipOrderNo);
            return pendingReason("SLIP_NOT_MATCH_ORDER");
        }

        Orders order = orderRepository.findByOrderNo(slipOrderNo).orElse(null);
        if (order == null)
            return pending();

        Double expectedAmount = order.getTotalAmountWithFee();
        Double amountPaid = parseDouble(getString(data, "amount"));

        if (expectedAmount == null || amountPaid == null)
            return pending();

        if (Math.abs(amountPaid - expectedAmount) > 1.0) {
            log.warn("[SLIP verify] amount mismatch orderNo={} paid={} expected={}",
                    slipOrderNo, amountPaid, expectedAmount);
            return pendingReason("AMOUNT_MISMATCH");
        }

        if (PaymentStatus.SUCCESS.toString().equalsIgnoreCase(order.getPaymentStatus())) {
            return success();
        }

        try {
            orderService.updatePaymentStatus(slipOrderNo, transRef, PaymentStatus.SUCCESS);
            Orders refreshed = orderRepository.findByOrderNo(slipOrderNo).orElse(order);
            webhookService.sendSuccessEmail(refreshed, transRef);
            return success();
        } catch (Exception ex) {
            log.error("[SLIP verify] settle failed orderNo={} transRef={}", slipOrderNo, transRef, ex);
            return pending();
        }
    }

    private String resolveSendingBank(String sendingBank) {
        String value = trimToNull(sendingBank);

        if (value == null) {
            return DEFAULT_SENDING_BANK;
        }

        if (!value.matches("\\d{3}")) {
            log.warn("[SLIP verify] invalid sendingBank='{}', fallback={}", sendingBank, DEFAULT_SENDING_BANK);
            return DEFAULT_SENDING_BANK;
        }

        return value;
    }

    private MiniQrInfo extractMiniQr(String miniQr) {
        String raw = trimToNull(miniQr);
        if (raw == null || raw.length() < 8) {
            return MiniQrInfo.empty();
        }

        Map<String, String> outer = parseTlvAsMap(raw);
        String payload = trimToNull(outer.get("00"));
        if (payload == null) {
            return MiniQrInfo.empty();
        }

        Map<String, String> sub = parseTlvAsMap(payload);

        String sendingBank = trimToNull(sub.get("01"));
        String transRef = trimToNull(sub.get("02"));

        if (sendingBank != null && !sendingBank.matches("\\d{3}")) {
            log.warn("[SLIP verify] invalid sendingBank in mini QR: {}", sendingBank);
            sendingBank = null;
        }

        if (transRef == null) {
            log.warn("[SLIP verify] transRef not found in mini QR");
            return MiniQrInfo.empty();
        }

        return new MiniQrInfo(sendingBank, transRef);
    }

    private Map<String, String> parseTlvAsMap(String input) {
        Map<String, String> out = new LinkedHashMap<>();
        if (input == null) {
            return out;
        }

        int i = 0;
        while (i + 4 <= input.length()) {
            String tag = input.substring(i, i + 2);
            Integer len = parseTlvLength(input, i);

            if (len == null) {
                return out;
            }

            int valueStart = i + 4;
            int valueEnd = valueStart + len;
            if (valueEnd > input.length()) {
                return out;
            }

            out.put(tag, input.substring(valueStart, valueEnd));
            i = valueEnd;
        }

        return out;
    }

    private Integer parseTlvLength(String input, int index) {
        try {
            return Integer.parseInt(input.substring(index + 2, index + 4));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private record MiniQrInfo(String sendingBank, String transRef) {
        static MiniQrInfo empty() {
            return new MiniQrInfo(null, null);
        }

        boolean hasTransRef() {
            return transRef != null && !transRef.isBlank();
        }
    }

    // ──────────────────────────────────────────────
    // Alipay+ / WeChatPay (e-Wallet via SCB API)
    // ──────────────────────────────────────────────

    public Map<String, Object> createEwalletQRCode(String tranType,
            String outTradeNo,
            String totalFee) {
        try {
            String accessToken = getAccessToken();
            String url = paymentConfig.getBaseUrl() + "/v1/payment/ewallets/qrcode/create";

            HttpHeaders headers = scbHeaders(accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("tranType", tranType);
            body.put("companyId", paymentConfig.getCompanyId());
            body.put("terminalId", paymentConfig.getTerminalId());
            body.put("outTradeNo", outTradeNo);
            body.put("totalFee", totalFee);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });
            Map<String, Object> responseBody = response.getBody();

            if (response.getStatusCode() == HttpStatus.OK && responseBody != null) {
                return responseBody;
            } else {
                log.error("[SCB eWallet] Failed to create eWallet QR code, status={}, body={}, outTradeNo={}", response.getStatusCode(), responseBody, outTradeNo);
                appErrorLogService.logBackendError("error", "SCB_EWALLET",
                        "Failed to create eWallet QR code, status=" + response.getStatusCode() + ", outTradeNo=" + outTradeNo, null);
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to create eWallet QR code");
                return error;
            }
        } catch (Exception ex) {
            log.error("[SCB eWallet] Error calling SCB eWallet QR API, tranType={}, outTradeNo={}", tranType, outTradeNo, ex);
            appErrorLogService.logBackendError("error", "SCB_EWALLET",
                    "Exception calling SCB eWallet QR API, tranType=" + tranType + ", outTradeNo=" + outTradeNo + ": " + ex.getMessage(),
                    Arrays.toString(ex.getStackTrace()));
            throw ex;
        }
    }

    public boolean inquireEwalletPayment(String tranType, String outTradeNo) {
        String url = paymentConfig.getBaseUrl() + "/v1/payment/ewallets/inquire";
        String accessToken = getAccessToken();

        HttpHeaders headers = scbHeaders(accessToken);

        Map<String, Object> body = new HashMap<>();
        body.put("tranType", tranType);
        body.put("companyId", paymentConfig.getCompanyId());
        body.put("terminalId", paymentConfig.getTerminalId());
        body.put("outTradeNo", outTradeNo);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                return false;
            }

            return processInquiryResponse(response.getBody(), outTradeNo);

        } catch (Exception ex) {
            log.error("[EWALLET inquire] downstream error outTradeNo={}", outTradeNo, ex);
            return false;
        }
    }

    private boolean processInquiryResponse(Map<String, Object> response, String outTradeNo) {
        if (response == null)
            return false;

        try {
            Map<?, ?> status = (response.get("status") instanceof Map<?, ?> m) ? m : null;
            Map<?, ?> data = (response.get("data") instanceof Map<?, ?> m) ? m : null;
            if (status == null || data == null)
                return false;

            String code = status.get("code") != null ? String.valueOf(status.get("code")) : null;
            if (!"1000".equals(code))
                return false;

            String tradeState = data.get("tradeState") != null ? String.valueOf(data.get("tradeState")) : null;
            String tsUpper = tradeState != null ? tradeState.toUpperCase() : "";

            boolean paid = "TRADE_FINISHED".equals(tsUpper) || "SUCCESS".equals(tsUpper);
            if (!paid)
                return false;

            String orderNo = extractOrderNoFromOutTradeNo(outTradeNo);
            if (orderNo == null || orderNo.isBlank()) {
                log.warn("[EWALLET inquire] Cannot extract orderNo from outTradeNo={}", outTradeNo);
                return false;
            }

            Orders order = orderRepository.findByOrderNo(orderNo).orElse(null);
            if (order == null)
                return false;

            if (PaymentStatus.SUCCESS.toString().equalsIgnoreCase(order.getPaymentStatus())) {
                return true;
            }

            String txnId = firstNonBlank(trimToNull(getString(data, "transactionId")), outTradeNo);

            orderService.updatePaymentStatus(orderNo, txnId, PaymentStatus.SUCCESS);

            Orders refreshed = orderRepository.findByOrderNo(orderNo).orElse(order);
            sendSuccessEmailSafely(refreshed, txnId, orderNo);

            return true;

        } catch (Exception e) {
            log.error("[EWALLET inquire] processInquiryResponse error outTradeNo={}", outTradeNo, e);
            return false;
        }
    }

    private String extractOrderNoFromOutTradeNo(String outTradeNo) {
        if (outTradeNo == null)
            return null;
        String s = outTradeNo.trim();
        if (s.length() == 20 && s.startsWith("SCB")) {
            String orderNo = s.substring(6);
            if (orderNo.length() == 14 && orderNo.startsWith("OR"))
                return orderNo;
        }
        return null;
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private HttpHeaders scbHeaders(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("authorization", "Bearer " + accessToken);
        headers.set("resourceOwnerId", paymentConfig.getAppKey());
        headers.set("requestUId", UUID.randomUUID().toString());
        headers.set("accept-language", "EN");
        return headers;
    }

    private Map<?, ?> asMap(Object o) {
        return (o instanceof Map<?, ?> m) ? m : null;
    }

    private String getString(Map<?, ?> map, String key) {
        if (map == null)
            return null;
        Object v = map.get(key);
        return v != null ? String.valueOf(v) : null;
    }

    private String trimToNull(String s) {
        if (s == null)
            return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private String firstNonBlank(String... vals) {
        if (vals == null)
            return null;
        for (String v : vals) {
            if (!isBlank(v))
                return v;
        }
        return null;
    }

    private Double parseDouble(String s) {
        if (s == null)
            return null;
        try {
            return Double.valueOf(s);
        } catch (Exception e) {
            return null;
        }
    }

    private Map<String, Object> pending() {
        return Map.of("settled", false, "status", "PENDING");
    }

    private Map<String, Object> pendingReason(String reason) {
        return Map.of("settled", false, "status", "PENDING", "reason", reason);
    }

    private Map<String, Object> success() {
        return Map.of("settled", true, "status", "SUCCESS");
    }

    private void sendSuccessEmailSafely(Orders order, String txnId, String orderNo) {
        try {
            webhookService.sendSuccessEmail(order, txnId);
        } catch (Exception mailEx) {
            log.error("[EWALLET inquire] email failed orderNo={} txnId={}", orderNo, txnId, mailEx);
        }
    }
}
