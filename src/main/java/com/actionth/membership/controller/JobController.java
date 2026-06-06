package com.actionth.membership.controller;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerMetaData;
import org.quartz.Trigger;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.actionth.membership.dto.JobExecutionHistoryResponse;
import com.actionth.membership.dto.JobMonitoringResponse;
import com.actionth.membership.dto.JobStatusResponse;
import com.actionth.membership.dto.JobTriggerResponse;
import com.actionth.membership.service.JobMonitoringService;

import lombok.extern.slf4j.Slf4j;

/**
 * Controller for manually triggering scheduled jobs
 */
@Slf4j
@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private JobMonitoringService jobMonitoringService;

    /**
     * Manually trigger the update overdue payments job
     * Requires admin role
     */
    @PostMapping("/trigger/update-overdue-payments")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobTriggerResponse> triggerUpdateOverduePaymentsJob() {
        try {
            log.info("Manual trigger requested for UpdateOverduePaymentsJob at {}", OffsetDateTime.now());
            
            JobKey jobKey = new JobKey("updateOverduePaymentsJob");
            scheduler.triggerJob(jobKey);
            
            log.info("UpdateOverduePaymentsJob triggered successfully");
            
            JobTriggerResponse response = JobTriggerResponse.builder()
                    .success(true)
                    .message("Job triggered successfully")
                    .jobName("updateOverduePaymentsJob")
                    .triggeredAt(OffsetDateTime.now().toString())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (SchedulerException e) {
            log.error("Failed to trigger UpdateOverduePaymentsJob", e);
            
            JobTriggerResponse response = JobTriggerResponse.builder()
                    .success(false)
                    .message("Failed to trigger job: " + e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get job status and next execution time
     */
    @PostMapping("/status/update-overdue-payments")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobStatusResponse> getJobStatus() {
        try {
            JobKey jobKey = new JobKey("updateOverduePaymentsJob");
            
            // Check if job exists
            boolean exists = scheduler.checkExists(jobKey);
            
            JobStatusResponse.JobStatusResponseBuilder responseBuilder = JobStatusResponse.builder()
                    .success(true)
                    .exists(exists)
                    .jobName("updateOverduePaymentsJob");
            
            if (exists) {
                // Get triggers for this job
                var triggers = scheduler.getTriggersOfJob(jobKey);
                if (!triggers.isEmpty()) {
                    var trigger = triggers.get(0);
                    responseBuilder
                            .nextFireTime(trigger.getNextFireTime())
                            .previousFireTime(trigger.getPreviousFireTime())
                            .triggerState(scheduler.getTriggerState(trigger.getKey()).name());
                }
            }
            
            return ResponseEntity.ok(responseBuilder.build());
            
        } catch (SchedulerException e) {
            log.error("Failed to get job status", e);
            
            JobStatusResponse response = JobStatusResponse.builder()
                    .success(false)
                    .message("Failed to get job status: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get comprehensive monitoring information for all jobs
     */
    @GetMapping("/monitoring")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobMonitoringResponse> getAllJobsMonitoring() {
        try {
            List<JobMonitoringResponse.JobInfo> jobInfoList = new ArrayList<>();
            
            SchedulerMetaData metaData = scheduler.getMetaData();
            
            for (String groupName : scheduler.getJobGroupNames()) {
                for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.jobGroupEquals(groupName))) {
                    JobDetail jobDetail = scheduler.getJobDetail(jobKey);
                    
                    List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                    
                    for (Trigger trigger : triggers) {
                        JobMonitoringResponse.JobInfo.JobInfoBuilder jobInfoBuilder = JobMonitoringResponse.JobInfo.builder()
                                .jobName(jobKey.getName())
                                .jobGroup(jobKey.getGroup())
                                .description(jobDetail.getDescription())
                                .triggerName(trigger.getKey().getName())
                                .triggerGroup(trigger.getKey().getGroup())
                                .triggerState(scheduler.getTriggerState(trigger.getKey()).name())
                                .nextFireTime(trigger.getNextFireTime())
                                .previousFireTime(trigger.getPreviousFireTime())
                                .startTime(trigger.getStartTime())
                                .endTime(trigger.getEndTime())
                                .priority(trigger.getPriority())
                                .misfireInstruction(trigger.getMisfireInstruction())
                                .mayFireAgain(trigger.mayFireAgain());
                        
                        if (trigger instanceof CronTrigger cronTrigger) {
                            jobInfoBuilder.cronExpression(cronTrigger.getCronExpression());
                        }
                        
                        jobInfoList.add(jobInfoBuilder.build());
                    }
                }
            }
            
            JobMonitoringResponse.SchedulerInfo schedulerInfo = JobMonitoringResponse.SchedulerInfo.builder()
                    .schedulerName(metaData.getSchedulerName())
                    .schedulerInstanceId(metaData.getSchedulerInstanceId())
                    .started(metaData.isStarted())
                    .inStandbyMode(metaData.isInStandbyMode())
                    .shutdown(metaData.isShutdown())
                    .runningSince(metaData.getRunningSince())
                    .numberOfJobsExecuted(metaData.getNumberOfJobsExecuted())
                    .jobStoreClass(metaData.getJobStoreClass().getSimpleName())
                    .supportsPersistence(metaData.isJobStoreSupportsPersistence())
                    .clustered(metaData.isJobStoreClustered())
                    .threadPoolSize(metaData.getThreadPoolSize())
                    .version(metaData.getVersion())
                    .build();
            
            JobMonitoringResponse response = JobMonitoringResponse.builder()
                    .success(true)
                    .jobs(jobInfoList)
                    .schedulerInfo(schedulerInfo)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (SchedulerException e) {
            log.error("Failed to get job monitoring info", e);
            
            JobMonitoringResponse response = JobMonitoringResponse.builder()
                    .success(false)
                    .message("Failed to get monitoring info: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get execution history from database
     */
    @GetMapping("/execution-history")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobExecutionHistoryResponse> getExecutionHistory() {
        try {
            List<JobExecutionHistoryResponse.ExecutionRecord> executions = 
                    jobMonitoringService.getRecentExecutions();
            
            JobExecutionHistoryResponse response = JobExecutionHistoryResponse.builder()
                    .success(true)
                    .executions(executions)
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Failed to get execution history", e);
            
            JobExecutionHistoryResponse response = JobExecutionHistoryResponse.builder()
                    .success(false)
                    .message("Failed to get execution history: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Manually trigger the resend failed emails job
     * Requires admin role
     */
    @PostMapping("/trigger/resend-failed-emails")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobTriggerResponse> triggerResendFailedEmailsJob() {
        try {
            log.info("Manual trigger requested for ResendFailedEmailsJob at {}", OffsetDateTime.now());
            
            JobKey jobKey = new JobKey("resendFailedEmailsJob");
            scheduler.triggerJob(jobKey);
            
            log.info("ResendFailedEmailsJob triggered successfully");
            
            JobTriggerResponse response = JobTriggerResponse.builder()
                    .success(true)
                    .message("Job triggered successfully")
                    .jobName("resendFailedEmailsJob")
                    .triggeredAt(OffsetDateTime.now().toString())
                    .build();
            
            return ResponseEntity.ok(response);
            
        } catch (SchedulerException e) {
            log.error("Failed to trigger ResendFailedEmailsJob", e);
            
            JobTriggerResponse response = JobTriggerResponse.builder()
                    .success(false)
                    .message("Failed to trigger job: " + e.getMessage())
                    .error(e.getClass().getSimpleName())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }

    /**
     * Get job status and next execution time for resend failed emails job
     */
    @PostMapping("/status/resend-failed-emails")
    @PreAuthorize("hasRole('admin')")
    public ResponseEntity<JobStatusResponse> getResendFailedEmailsJobStatus() {
        try {
            JobKey jobKey = new JobKey("resendFailedEmailsJob");
            
            // Check if job exists
            boolean exists = scheduler.checkExists(jobKey);
            
            JobStatusResponse.JobStatusResponseBuilder responseBuilder = JobStatusResponse.builder()
                    .success(true)
                    .exists(exists)
                    .jobName("resendFailedEmailsJob");
            
            if (exists) {
                // Get triggers for this job
                var triggers = scheduler.getTriggersOfJob(jobKey);
                if (!triggers.isEmpty()) {
                    var trigger = triggers.get(0);
                    responseBuilder
                            .nextFireTime(trigger.getNextFireTime())
                            .previousFireTime(trigger.getPreviousFireTime())
                            .triggerState(scheduler.getTriggerState(trigger.getKey()).name());
                }
            }
            
            return ResponseEntity.ok(responseBuilder.build());
            
        } catch (SchedulerException e) {
            log.error("Failed to get resend failed emails job status", e);
            
            JobStatusResponse response = JobStatusResponse.builder()
                    .success(false)
                    .message("Failed to get job status: " + e.getMessage())
                    .build();
            
            return ResponseEntity.internalServerError().body(response);
        }
    }
}
