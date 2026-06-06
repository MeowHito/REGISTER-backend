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
public class JobMonitoringResponse {
    private boolean success;
    private String message;
    private List<JobInfo> jobs;
    private SchedulerInfo schedulerInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class JobInfo {
        private String jobName;
        private String jobGroup;
        private String description;
        private String triggerName;
        private String triggerGroup;
        private String triggerState;
        private String cronExpression;
        private Date nextFireTime;
        private Date previousFireTime;
        private Date startTime;
        private Date endTime;
        private Integer priority;
        private Integer misfireInstruction;
        private boolean mayFireAgain;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SchedulerInfo {
        private String schedulerName;
        private String schedulerInstanceId;
        private boolean started;
        private boolean inStandbyMode;
        private boolean shutdown;
        private Date runningSince;
        private int numberOfJobsExecuted;
        private String jobStoreClass;
        private boolean supportsPersistence;
        private boolean clustered;
        private int threadPoolSize;
        private String version;
    }
}
