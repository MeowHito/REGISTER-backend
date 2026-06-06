package com.actionth.membership.model.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventTypeAvailabilityResponse {
    private String eventTypeId;
    private String pricingId;
    private String name;
    private Integer eventTypeQuota;
    private Integer totalQuota;
    private Integer availableQuota;
    private Integer registeredCount;
    private Boolean isAvailable;
    private BigDecimal currentPrice;
    private String paymentName;
    private Boolean isSpecialPrice;
}
