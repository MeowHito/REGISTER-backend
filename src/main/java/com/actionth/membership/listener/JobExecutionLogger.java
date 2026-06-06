package com.actionth.membership.listener;

import java.time.OffsetDateTime;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.actionth.membership.model.JobExecutionLog;
import com.actionth.membership.repository.JobExecutionLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz JobListener to log all job executions to database
 */
@Slf4j
@Component
public class JobExecutionLogger implements JobListener {

    @Autowired
    private JobExecutionLogRepository jobExecutionLogRepository;

    @Override
    public String getName() {
        return "JobExecutionLogger";
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        try {
            JobExecutionLog logEntry = JobExecutionLog.builder()
                    .jobName(context.getJobDetail().getKey().getName())
                    .jobGroup(context.getJobDetail().getKey().getGroup())
                    .triggerName(context.getTrigger().getKey().getName())
                    .triggerGroup(context.getTrigger().getKey().getGroup())
                    .instanceName(context.getScheduler().getSchedulerInstanceId())
                    .firedTime(OffsetDateTime.now())
                    .scheduledTime(OffsetDateTime.ofInstant(
                            context.getScheduledFireTime().toInstant(),
                            java.time.ZoneId.systemDefault()))
                    .status("STARTED")
                    .priority(context.getTrigger().getPriority())
                    .createdAt(OffsetDateTime.now())
                    .build();

            // Store the log entry in the context for later update
            context.put("logEntry", jobExecutionLogRepository.save(logEntry));
            
            log.debug("Job execution started: {} - {}", 
                    logEntry.getJobName(), logEntry.getFiredTime());
            
        } catch (Exception e) {
            log.error("Failed to log job execution start", e);
        }
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        log.warn("Job execution vetoed: {}", context.getJobDetail().getKey().getName());
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        try {
            JobExecutionLog logEntry = (JobExecutionLog) context.get("logEntry");
            
            if (logEntry != null) {
                logEntry.setCompletedTime(OffsetDateTime.now());
                logEntry.setDurationMs(
                        logEntry.getCompletedTime().toInstant().toEpochMilli() - 
                        logEntry.getFiredTime().toInstant().toEpochMilli());
                
                if (jobException != null) {
                    logEntry.setStatus("FAILED");
                    logEntry.setErrorMessage(jobException.getMessage());
                    log.error("Job execution failed: {} - {}", 
                            logEntry.getJobName(), jobException.getMessage());
                } else {
                    logEntry.setStatus("COMPLETED");
                    log.debug("Job execution completed: {} - Duration: {}ms", 
                            logEntry.getJobName(), logEntry.getDurationMs());
                }
                
                jobExecutionLogRepository.save(logEntry);
            }
            
        } catch (Exception e) {
            log.error("Failed to log job execution completion", e);
        }
    }
}
