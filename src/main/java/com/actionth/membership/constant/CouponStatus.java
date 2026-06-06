package com.actionth.membership.constant;

public enum CouponStatus {
    NEW("new"),
    APPROVED("approved"),
    REDEEMED("redeemed")
    ;

    private final String description;

    CouponStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
