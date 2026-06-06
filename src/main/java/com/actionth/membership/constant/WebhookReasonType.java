package com.actionth.membership.constant;

public final class WebhookReasonType {

    private WebhookReasonType() {}

    // --- Order-status based reasons ---
    public static final String DUPLICATE_AFTER_SUCCESS = "DUPLICATE_AFTER_SUCCESS";
    public static final String PAYMENT_AFTER_FAILED = "PAYMENT_AFTER_FAILED";
    public static final String PAYMENT_AFTER_CANCELLED = "PAYMENT_AFTER_CANCELLED";
    public static final String UNKNOWN_STATUS = "UNKNOWN_STATUS";
    public static final String PAYMENT_METHOD_MISMATCH = "PAYMENT_METHOD_MISMATCH";
    public static final String AMOUNT_MISMATCH = "AMOUNT_MISMATCH";
    public static final String UNKNOWN_PAYMENT_METHOD = "UNKNOWN_PAYMENT_METHOD";

    // --- 2C2P gateway categories ---
    public static final String CANCELLED = "CANCELLED";
    public static final String SOFT_DECLINED_3DS = "SOFT_DECLINED_3DS";
    public static final String AUTH_FAILED = "AUTH_FAILED";
    public static final String NOT_FOUND = "NOT_FOUND";
    public static final String INQUIRY_FAILED = "INQUIRY_FAILED";
    public static final String SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String TIMEOUT = "TIMEOUT";
    public static final String FAILED = "FAILED";
    public static final String UNKNOWN = "UNKNOWN";

    /**
     * Check if a 2C2P respCode represents an anomaly (not success/pending).
     */
    public static boolean is2c2pAnomaly(String respCode) {
        String code = (respCode == null) ? "" : respCode.trim();
        return !("0000".equals(code) || "0001".equals(code) || "2001".equals(code));
    }

    /**
     * Build a composite reasonType string from a 2C2P respCode, e.g. "4081_AUTH_FAILED".
     */
    public static String build2c2pReasonType(String respCode) {
        String code = (respCode == null || respCode.isBlank()) ? UNKNOWN : respCode.trim();
        return code + "_" + map2c2pCategory(code);
    }

    private static String map2c2pCategory(String respCode) {
        String code = (respCode == null) ? "" : respCode.trim();
        if (code.isBlank())
            return UNKNOWN;

        return switch (code) {
            case "0003", "4080" -> CANCELLED;
            case "0004" -> SOFT_DECLINED_3DS;
            case "4081" -> AUTH_FAILED;
            case "2002" -> NOT_FOUND;
            case "2003" -> INQUIRY_FAILED;
            case "0999", "9999", "4050", "4096", "5998" -> SYSTEM_ERROR;
            case "5002" -> TIMEOUT;
            default -> FAILED;
        };
    }
}
