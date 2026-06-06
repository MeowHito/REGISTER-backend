package com.actionth.membership.service;

import java.sql.Timestamp;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.actionth.membership.dto.JobExecutionHistoryResponse;
import com.actionth.membership.repository.JobExecutionLogRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for monitoring Quartz job executions
 */
@Slf4j
@Service
public class JobMonitoringService {

    @Autowired
    private JobExecutionLogRepository jobExecutionLogRepository;

    /**
     * Get recent job executions from custom log table
     */
    public List<JobExecutionHistoryResponse.ExecutionRecord> getRecentExecutions() {
        try {
            return jobExecutionLogRepository.findTop50ByOrderByFiredTimeDesc()
                    .stream()
                    .map(log -> JobExecutionHistoryResponse.ExecutionRecord.builder()
                            .entryId(log.getId().toString())
                            .jobName(log.getJobName())
                            .jobGroup(log.getJobGroup())
                            .triggerName(log.getTriggerName())
                            .triggerGroup(log.getTriggerGroup())
                            .instanceName(log.getInstanceName())
                            .firedTime(Timestamp.valueOf(log.getFiredTime().toLocalDateTime()))
                            .scheduledTime(Timestamp.valueOf(log.getScheduledTime().toLocalDateTime()))
                            .state(log.getStatus())
                            .priority(log.getPriority())
                            .build())
                    .toList();
            
        } catch (Exception e) {
            log.error("Error fetching recent executions", e);
            throw new RuntimeException("Failed to fetch execution history", e);
        }
    }

    /**
     * Get execution history for a specific job
     */
    public List<JobExecutionHistoryResponse.ExecutionRecord> getExecutionHistoryByJobName(String jobName) {
        try {
            return jobExecutionLogRepository.findByJobNameOrderByFiredTimeDesc(jobName)
                    .stream()
                    .map(log -> JobExecutionHistoryResponse.ExecutionRecord.builder()
                            .entryId(log.getId().toString())
                            .jobName(log.getJobName())
                            .jobGroup(log.getJobGroup())
                            .triggerName(log.getTriggerName())
                            .triggerGroup(log.getTriggerGroup())
                            .instanceName(log.getInstanceName())
                            .firedTime(Timestamp.valueOf(log.getFiredTime().toLocalDateTime()))
                            .scheduledTime(Timestamp.valueOf(log.getScheduledTime().toLocalDateTime()))
                            .state(log.getStatus())
                            .priority(log.getPriority())
                            .build())
                    .toList();
            
        } catch (Exception e) {
            log.error("Error fetching execution history for job: {}", jobName, e);
            throw new RuntimeException("Failed to fetch execution history for job: " + jobName, e);
        }
    }
}
