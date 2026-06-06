package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailQueueDashboardDto {
    private long totalQueued;
    private long pendingCount;
    private long sentCount;
    private long failedCount;

    // Queue sub-limit (CORRECTION + RETRY only, from emailQueue table)
    private long queueSentToday;
    private int queueDailyLimit;
    private int queueRemainingToday;

    // Global limit (all system emails, from emailLog table)
    private long globalSentToday;
    private int globalDailyLimit;
    private int globalRemainingToday;
}
