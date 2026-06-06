package com.actionth.membership.service;

import com.actionth.membership.config.PaymentConfig;
import com.actionth.membership.exception.BusinessException;
import com.actionth.membership.model.Orders;
import com.actionth.membership.model.dto.OrderUpdateResponse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Payment Gateway Service — facade that delegates to provider-specific
 * services.
 *
 * <ul>
 * <li><b>SCB</b> → QR Code (PromptPay), Slip Verify, Alipay+, WeChatPay</li>
 * <li><b>2C2P</b> → Credit/Debit Card, E-wallet (LINE Pay, TrueMoney)</li>
 * </ul>
 */
@Service
@Slf4j
public class PaymentGatewayService {

    private final SCBPaymentService scbPaymentService;
    private final TwoCTwoPPaymentService twoCTwoPPaymentService;
    private final PaymentConfig paymentConfig;

    public PaymentGatewayService(SCBPaymentService scbPaymentService,
            TwoCTwoPPaymentService twoCTwoPPaymentService,
            PaymentConfig paymentConfig) {
        this.scbPaymentService = scbPaymentService;
        this.twoCTwoPPaymentService = twoCTwoPPaymentService;
        this.paymentConfig = paymentConfig;
    }

    public OrderUpdateResponse.PaymentData initiatePayment(Orders order) {
        String paymentMethod = order.getPaymentMethod();
        String orderNo = order.getOrderNo();
        Double totalAmountWithFee = order.getTotalAmountWithFee();
        String amount = String.format("%.2f", totalAmountWithFee);

        log.info("[InitiatePayment] Starting payment initiation: orderNo={}, method={}, amount={}",
                orderNo, paymentMethod, amount);

        try {
            switch (paymentMethod) {
                case "qrcode":
                    return initiateQRCodePayment(order, orderNo, amount);

                case "alipay":
                    return initiateEwalletQRPayment(order, orderNo, amount, "A");

                case "wechatpay":
                    return initiateEwalletQRPayment(order, orderNo, amount, "W");

                case "creditcard":
                    return initiate2C2PPayment(orderNo, amount, new String[]{"CC"});

                case "ewallet":
                    return initiate2C2PPayment(orderNo, amount, new String[]{"LINE", "TRUEMONEY"});

                default:
                    log.warn("[InitiatePayment] Unknown payment method: {}", paymentMethod);
                    return null;
            }
        } catch (Exception e) {
            log.error("[InitiatePayment] Failed to initiate payment: orderNo={}, method={}", orderNo, paymentMethod, e);
            throw e;
        }
    }

    private OrderUpdateResponse.PaymentData initiateQRCodePayment(Orders order, String orderNo, String amount) {
        String ref2 = generateRef2();
        String ref3 = paymentConfig.getRef3();

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("qrType", "PP");
        request.put("ppType", "BILLERID");
        request.put("ppId", paymentConfig.getPpId());
        request.put("amount", amount);
        request.put("ref1", orderNo);
        request.put("ref2", ref2);
        request.put("ref3", ref3);

        Map<String, Object> response = createQRCode(request, order);

        boolean success = Boolean.TRUE.equals(response.get("success"))
                || isDescriptionSuccess(response);

        if (success) {
            Map<?, ?> data = (response.get("data") instanceof Map<?, ?> m) ? m : null;
            String qrImage = data != null ? String.valueOf(data.get("qrImage")) : null;

            return OrderUpdateResponse.PaymentData.builder()
                    .paymentMethod("qrcode")
                    .qrImage(qrImage)
                    .refNo(orderNo)
                    .build();
        }

        throw new BusinessException("Failed to create QR Code: " + response.get("message"));
    }

    private OrderUpdateResponse.PaymentData initiateEwalletQRPayment(Orders order, String orderNo, String amount, String tranType) {
        String outTradeNo = "SCB" + randomAlphaNum(3) + orderNo;

        Map<String, Object> response = createEwalletQRCode(tranType, outTradeNo, amount);

        boolean success = isDescriptionSuccess(response);

        if (success) {
            Map<?, ?> data = (response.get("data") instanceof Map<?, ?> m) ? m : null;
            String codeUrl = data != null ? String.valueOf(data.get("codeUrl")) : null;

            return OrderUpdateResponse.PaymentData.builder()
                    .paymentMethod("A".equals(tranType) ? "alipay" : "wechatpay")
                    .qrImage(codeUrl)
                    .refNo(orderNo)
                    .outTradeNo(outTradeNo)
                    .tranType(tranType)
                    .build();
        }

        throw new BusinessException("Failed to create eWallet QR Code: " + response.get("message"));
    }

    private OrderUpdateResponse.PaymentData initiate2C2PPayment(String orderNo, String amount, String[] channels) {
        Map<String, Object> response = twoCTwoPPaymentService.createPaymentToken(orderNo, amount, channels);

        String respCode = (String) response.get("respCode");
        String respDesc = (String) response.get("respDesc");
        String webPaymentUrl = ("Success".equals(respDesc))
                ? (String) response.get("webPaymentUrl") : null;
        String payload = (String) response.get("payload");

        String paymentMethod = (channels.length > 0 && "CC".equals(channels[0])) ? "creditcard" : "ewallet";

        return OrderUpdateResponse.PaymentData.builder()
                .paymentMethod(paymentMethod)
                .webPaymentUrl(webPaymentUrl)
                .payload(payload)
                .respCode(respCode)
                .refNo(orderNo)
                .build();
    }

    // ──────────────────────────────────────────────
    // SCB — QR Code (PromptPay)
    // ──────────────────────────────────────────────

    public Map<String, Object> createQRCode(Map<String, Object> request, Orders order) {
        return scbPaymentService.createQRCode(request, order);
    }

    // ──────────────────────────────────────────────
    // SCB — Slip Verify
    // ──────────────────────────────────────────────

    public Map<String, Object> verifyTransaction(String slipQrRawData, String expectedOrderNo) {
        return scbPaymentService.verifyTransaction(slipQrRawData, expectedOrderNo);
    }

    // ──────────────────────────────────────────────
    // SCB — Alipay+ / WeChatPay
    // ──────────────────────────────────────────────

    public Map<String, Object> createEwalletQRCode(String tranType, String outTradeNo, String totalFee) {
        return scbPaymentService.createEwalletQRCode(tranType, outTradeNo, totalFee);
    }

    public boolean inquireEwalletPayment(String tranType, String outTradeNo) {
        return scbPaymentService.inquireEwalletPayment(tranType, outTradeNo);
    }

    // ──────────────────────────────────────────────
    // 2C2P — Credit/Debit Card, LINE Pay, TrueMoney
    // ──────────────────────────────────────────────

    public Map<String, Object> inquire2C2PPayment(String invoiceNo, String paymentType) {
        return twoCTwoPPaymentService.inquirePayment(invoiceNo, paymentType);
    }

    // ──────────────────────────────────────────────
    // 2C2P — Payment Token
    // ──────────────────────────────────────────────

    public Map<String, Object> createPaymentToken(String invoiceNo, String amount, String[] paymentChannels) {
        return twoCTwoPPaymentService.createPaymentToken(invoiceNo, amount, paymentChannels);
    }

    // ──────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────

    private String generateRef2() {
        LocalDateTime now = LocalDateTime.now();
        return now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private String randomAlphaNum(int len) {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            sb.append(chars.charAt(ThreadLocalRandom.current().nextInt(chars.length())));
        }
        return sb.toString();
    }

    private boolean isDescriptionSuccess(Map<String, Object> response) {
        Object statusObj = response.get("status");
        if (statusObj instanceof Map<?, ?> statusMap) {
            Object desc = statusMap.get("description");
            return desc != null && "success".equalsIgnoreCase(String.valueOf(desc));
        }
        return false;
    }
}
