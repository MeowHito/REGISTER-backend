package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingConfirmationDto {

    private String orderNo;
    private String eventName;
    private String customerEmail;
    private String paidAt;
    private String emailStatus;
    private int failedCount;
}
