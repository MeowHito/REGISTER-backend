package com.actionth.membership.service;

import com.actionth.membership.model.dto.AvailablePricingResponse;

public interface PricingService {

    Boolean checkQuota(String pricingId);
    
    AvailablePricingResponse getAvailablePricingByEventType(String eventTypeId);
}
