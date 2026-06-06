package com.actionth.membership.projection;

import java.math.BigDecimal;

public interface PricingAvailabilityProjection {
    String getPricingUuid();
    BigDecimal getPrice();
    Integer getQuota();
    String getPaymentName();
    Long getUsedQuota();
}
