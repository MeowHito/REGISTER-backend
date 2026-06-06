package com.actionth.membership.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueDetailSummaryDTO {

    private String id;
    private String eventName;
    private String orderId;
    private String transactionId;
    private String paymentDateTime;
    private String fullName;
    private BigDecimal registrationFee;
    private BigDecimal discountCoupon;
    private BigDecimal discountShirt;
    private BigDecimal shippingFee;
    private BigDecimal serviceFee;
    private BigDecimal totalAmountWithFee;
    private BigDecimal totalAmount;
    private String eventTypeName;
    private String paymentStatus;
    private String paymentMethod;
    private String registrationDateTime;

}
