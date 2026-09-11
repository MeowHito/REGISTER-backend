package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResolveReviewResponse {

    private boolean success;
    private String outcome;
    private String message;
    private String orderNo;
    private ReviewContextDto.OrderState newState;
}
