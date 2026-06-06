package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentRateDTORequest {
    
    private String uuid;
    private Integer eventTypeId;
    private Integer payTypeId;
    
    private EventTypeDTORequest eventType;
    private PayTypeDTORequest payType;

    private Double price;
    private Double capacity;
}
