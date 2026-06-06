package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuotaValidationError {
    private String eventTypeName;
    private String pricingName;
    private Boolean isSpecialPrice;
    private Integer availableQuota;
    private Integer requestedQuota;
    private String errorCode;
    private String message;
}
