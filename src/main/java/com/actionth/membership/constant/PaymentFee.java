package com.actionth.membership.constant;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Set;

/**
 * Action's service fee per payment channel. The frontend shows the same table
 * (RegistrationPayment/utils.js calculatePaymentFeePercent) — keep them in step, but the
 * amount charged is always the one computed here.
 */
public final class PaymentFee {

    private PaymentFee() {}

    private static final Set<String> QR_CHANNELS = Set.of("qrcode");
    private static final Set<String> CARD_CHANNELS = Set.of("creditcard", "ewallet", "alipay", "wechatpay");

    /** Fee in percent (3 = 3%) for a payment method; 0 for free / unknown methods. */
    public static BigDecimal percentFor(String paymentMethod) {
        String method = paymentMethod == null ? "" : paymentMethod.trim().toLowerCase();
        if (QR_CHANNELS.contains(method)) {
            return BigDecimal.valueOf(3);
        }
        if (CARD_CHANNELS.contains(method)) {
            return BigDecimal.valueOf(5);
        }
        return BigDecimal.ZERO;
    }

    /** Fee on an amount, rounded up to the satang like the frontend (Math.ceil on cents). */
    public static BigDecimal feeOn(BigDecimal amount, BigDecimal percent) {
        return amount.multiply(percent).divide(BigDecimal.valueOf(100), 2, RoundingMode.CEILING);
    }
}
