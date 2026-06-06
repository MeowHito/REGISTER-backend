package com.actionth.membership.model.dto;

import java.time.OffsetDateTime;
import lombok.Data;

import org.springframework.format.annotation.DateTimeFormat;

@Data
public class OrderHistoryResponse {
    private String orderNo;
    private String status;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdTime;

    private Double amount;
    private String eventName;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private String id;
    private String reviewReason;

}