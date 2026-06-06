package com.actionth.membership.constant;

public final class WebhookDescription {

    private WebhookDescription() {}

    // --- Log type descriptions ---
    public static final String RECEIVED_WEBHOOK = "Received %s webhook callback";

    // --- Anomaly reason descriptions ---
    public static final String DESC_DUPLICATE_AFTER_SUCCESS = "Duplicate webhook received — order was already paid successfully";
    public static final String DESC_PAYMENT_AFTER_FAILED = "Webhook received for an order that was previously marked as failed";
    public static final String DESC_PAYMENT_AFTER_CANCELLED = "Webhook received for an order that was already cancelled";
    public static final String DESC_UNKNOWN_STATUS = "Webhook received but order status could not be determined";

    // --- 2C2P gateway descriptions ---
    public static final String DESC_2C2P_CANCELLED = "Payment was cancelled by the customer";
    public static final String DESC_2C2P_SOFT_DECLINED_3DS = "3D Secure verification failed — card was soft-declined";
    public static final String DESC_2C2P_AUTH_FAILED = "Card authentication failed";
    public static final String DESC_2C2P_NOT_FOUND = "Transaction not found at payment gateway";
    public static final String DESC_2C2P_INQUIRY_FAILED = "Payment status inquiry failed at gateway";
    public static final String DESC_2C2P_SYSTEM_ERROR = "Payment gateway returned a system error";
    public static final String DESC_2C2P_TIMEOUT = "Payment request timed out at gateway";
    public static final String DESC_2C2P_FAILED = "Payment was declined by the gateway";

    // --- Template ---
    public static final String ANOMALY_TEMPLATE = "[%s] Order %s — %s";

    public static String formatAnomaly(String provider, String orderNo, String reason) {
        return String.format(ANOMALY_TEMPLATE, provider, orderNo, reason);
    }

    public static String formatWebhook(String provider) {
        return String.format(RECEIVED_WEBHOOK, provider);
    }

    /**
     * Build a full human-readable anomaly description from a raw reasonType.
     * Maps the reasonType to a readable message, then wraps it with provider/orderNo context.
     */
    public static String buildDescription(String provider, String reasonType, String orderNo) {
        String reason = resolveReasonDescription(provider, reasonType);
        return formatAnomaly(provider, orderNo, reason);
    }

    /**
     * Resolve a raw reasonType to a human-readable description string.
     */
    public static String resolveReasonDescription(String provider, String reasonType) {
        if (reasonType == null) return "Unknown";
        return switch (reasonType) {
            case WebhookReasonType.DUPLICATE_AFTER_SUCCESS -> DESC_DUPLICATE_AFTER_SUCCESS;
            case WebhookReasonType.PAYMENT_AFTER_FAILED -> DESC_PAYMENT_AFTER_FAILED;
            case WebhookReasonType.PAYMENT_AFTER_CANCELLED -> DESC_PAYMENT_AFTER_CANCELLED;
            case WebhookReasonType.UNKNOWN_STATUS -> DESC_UNKNOWN_STATUS;
            default -> describeCodedReason(provider, reasonType);
        };
    }

    private static String describeCodedReason(String provider, String reasonType) {
        if (PaymentProvider.TWO_C2P.equals(provider)) {
            if (reasonType.contains(WebhookReasonType.CANCELLED)) return DESC_2C2P_CANCELLED;
            if (reasonType.contains(WebhookReasonType.SOFT_DECLINED_3DS)) return DESC_2C2P_SOFT_DECLINED_3DS;
            if (reasonType.contains(WebhookReasonType.AUTH_FAILED)) return DESC_2C2P_AUTH_FAILED;
            if (reasonType.contains(WebhookReasonType.NOT_FOUND)) return DESC_2C2P_NOT_FOUND;
            if (reasonType.contains(WebhookReasonType.INQUIRY_FAILED)) return DESC_2C2P_INQUIRY_FAILED;
            if (reasonType.contains(WebhookReasonType.SYSTEM_ERROR)) return DESC_2C2P_SYSTEM_ERROR;
            if (reasonType.contains(WebhookReasonType.TIMEOUT)) return DESC_2C2P_TIMEOUT;
            if (reasonType.contains(WebhookReasonType.FAILED)) return DESC_2C2P_FAILED;
        }
        return "Unexpected webhook event: " + reasonType;
    }
}
