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
    private List<OrderAddOnDto> addOns;
    private Double addOnTotal;
    /** Money snapshot of the order, so the history page shows what was actually charged. */
    private Double totalPrice;
    private Double couponDiscount;
    private Double feePercent;
    private Double fee;
    private Double totalAmountWithFee;
    private Boolean testMode;
    /** Where the confirmation email went (the buyer); only on full access, never to a read-only organizer. */
    private String confirmationEmail;
}