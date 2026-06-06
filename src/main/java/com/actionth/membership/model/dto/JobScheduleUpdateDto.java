package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobScheduleUpdateDto {
    private boolean success;
    private String message;
    private String jobName;
    private String cronExpression;
    private String timezone;
    private String nextFireTime;
}
