package com.actionth.membership.model.request;

import java.util.List;
import lombok.Data;

@Data
public class OrderUpdateRequest {
    private String orderId;
    private String paymentMethod;
    private Double couponDiscount;
    private String couponType;
    private String couponCode;
    private Double totalPrice;
    private Double fee;
    private Double feePercent;
    private Double totalAmountWithFee;
    private List<RunnerCouponDto> runnerCoupons;

    @Data
    public static class RunnerCouponDto {
        private String idNo;
        private Double couponDiscount;
        private Double netPrice;
    }
}