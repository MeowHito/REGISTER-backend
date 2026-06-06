package com.actionth.membership.model.request;

import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class CouponDTORequest {

    @NotBlank(message = "couponCode is required")
    private String couponCode;

    @NotBlank(message = "eventId is required")
    private String eventId;

    @NotEmpty(message = "idNo list cannot be empty")
    private List<String> idNo;

    private String orderId;
    
}
