package com.actionth.membership.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueSummaryDTO {
    private String contractNo;
    private String eventName;
    private String paymentMethod;
    private BigDecimal registrationFee;
    private BigDecimal serviceFee;
    private BigDecimal total;
    private BigDecimal shippingFee;
    private BigDecimal totalWithShipping;

}
