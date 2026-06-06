package com.actionth.membership.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobStatusResponse {
    private boolean success;
    private String message;
    private String jobName;
    private boolean exists;
    private Date nextFireTime;
    private Date previousFireTime;
    private String triggerState;
}
