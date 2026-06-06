package com.actionth.membership.dto;

import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionHistoryResponse {
    private boolean success;
    private String message;
    private List<ExecutionRecord> executions;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionRecord {
        private String entryId;
        private String jobName;
        private String jobGroup;
        private String triggerName;
        private String triggerGroup;
        private String instanceName;
        private Date firedTime;
        private Date scheduledTime;
        private String state;
        private Integer priority;
    }
}
