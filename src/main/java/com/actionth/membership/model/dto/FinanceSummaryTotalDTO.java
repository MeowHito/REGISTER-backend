package com.actionth.membership.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceSummaryTotalDTO {
    private BigDecimal totalDiscountCoupon;
    private BigDecimal totalDiscountShirt;
    private BigDecimal totalShippingFee;
    private BigDecimal totalAmount;
    private BigDecimal totalNetAmount;
    private BigDecimal totalServiceFee;
    private BigDecimal totalAmountWithFee;
}
