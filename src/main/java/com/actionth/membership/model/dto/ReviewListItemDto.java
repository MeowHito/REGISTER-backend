package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewListItemDto {

    private String orderNo;
    private String eventName;
    private String reviewReason;
    private String paymentMethod;
    private Double totalAmountWithFee;
    private OffsetDateTime paymentDueDatetime;
    private OffsetDateTime createdTime;
}
