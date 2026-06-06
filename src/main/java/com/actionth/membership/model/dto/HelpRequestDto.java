package com.actionth.membership.model.dto;

import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class HelpRequestDto {
    private String uuid;
    private String orderUuid;
    private String orderNo;
    private String message;
    private String status;
    private String adminNote;
    private OffsetDateTime createdTime;
    private OffsetDateTime updatedTime;
    private String requesterName;
    private String requesterEmail;
    private String attachmentUrl;
}
