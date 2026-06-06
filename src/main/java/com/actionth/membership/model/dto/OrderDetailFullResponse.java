package com.actionth.membership.model.dto;

import lombok.Data;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

@Data
public class OrderDetailFullResponse {
    private String orderNo;
    private String status;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime createdTime;
    private String paymentToken;
    private String eventId;
    private String eventName;
    private String eventLink;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime eventDate;
    private List<OrderItemResponse> details;
    private String uuid;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime paymentDueDatetime;
    private String paymentMethod;
    private String ownerUuid;
    private String reviewReason;
}