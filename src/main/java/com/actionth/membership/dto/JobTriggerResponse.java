package com.actionth.membership.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobTriggerResponse {
    private boolean success;
    private String message;
    private String jobName;
    private String triggeredAt;
    private String error;
}
