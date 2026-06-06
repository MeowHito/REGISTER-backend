package com.actionth.membership.model.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FinanceSummaryDTO {
    private String eventTypeName;
    private BigDecimal registrationFee;
    private BigDecimal qty;
    private BigDecimal total;

}
