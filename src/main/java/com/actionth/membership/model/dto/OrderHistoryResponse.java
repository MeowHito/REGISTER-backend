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

    // For the admin list, so a registration can be read without opening it.
    private String paymentMethod;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime paymentDateTime;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private OffsetDateTime paymentDueDatetime;
    private String buyerName;
    private String buyerEmail;
    private java.util.List<Applicant> applicants;

    @Data
    public static class Applicant {
        private String name;
        private String nameEn;
        private String eventTypeName;
        private String bibNo;
        private String phone;
        private Boolean isSelf;
    }
}