package com.actionth.membership.model.request;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolveReviewRequest {

    private String transactionId;
    private String overrideMethod;
    private Double overrideAmount;
    private boolean sendEmail;
    private String reason;
    private boolean confirmDoublePay;
}
