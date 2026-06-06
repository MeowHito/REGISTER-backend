package com.actionth.membership.constant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum PaymentStatus {
    PENDING("PENDING"),
    SUCCESS("SUCCESS"),
    FAILED("FAILED"),
    CANCELLED("CANCELLED"),
    REVIEW("REVIEW");

    private static final Logger logger = LoggerFactory.getLogger(PaymentStatus.class);

    private final String json;
    PaymentStatus(String json) { this.json = json; }
    @JsonValue public String getJson() { return json; }

    @JsonCreator
    public static PaymentStatus from(String raw) {
        if (raw == null) {
            logger.warn("Null payment status from gateway, using PENDING as default");
            return PENDING;
        }
        String v = raw.trim().toUpperCase().replace(' ', '_');

        return switch (v) {
            case "APPROVED", "SUCCESS" -> SUCCESS;
            case "WAIT_BUYER_PAY", "USERPAYING", "UNKNOWN", "PENDING" -> PENDING;
            case "NOT_PAID", "FAIL", "FAILED" -> FAILED;
            case "CANCEL", "CANCELED", "CANCELLED" -> CANCELLED;
            default-> {
                logger.warn("Unknown payment status from gateway: '{}', using PENDING as default", raw);
                yield PENDING;
            }
        };
    }
}
