package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueDto {
    private Integer id;
    private String uuid;
    private String type;
    private String orderNo;
    private String orderUuid;
    private String eventName;
    private String eventUuid;
    private String recipientEmail;
    private String subject;
    private Long emailLogId;
    private String status;
    private Integer retryCount;
    private OffsetDateTime createdTime;
    private OffsetDateTime processedAt;
    private String errorMessage;
    private boolean alreadySent;
}
