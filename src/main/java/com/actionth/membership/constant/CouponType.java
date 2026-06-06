package com.actionth.membership.constant;

public enum CouponType {
    EXTERNAL("external"),
    INTERNAL("internal"),
    REUSABLE("reusable"),
    NON_REUSABLE("non reusable");

    private final String description;

    CouponType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
