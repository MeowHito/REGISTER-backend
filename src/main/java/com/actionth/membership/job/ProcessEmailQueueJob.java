package com.actionth.membership.job;

import java.time.OffsetDateTime;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.actionth.membership.service.EmailQueueService;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Job to process the general email queue.
 * Respects the daily email limit and sends all pending emails regardless of type.
 * Runs every 30 minutes by default.
 */
@Slf4j
@Component
public class ProcessEmailQueueJob implements Job {

    @Autowired
    private EmailQueueService emailQueueService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting ProcessEmailQueueJob at {}", OffsetDateTime.now());

            // Reset stale PROCESSING items (stuck > 30 min) back to PENDING before processing
            emailQueueService.cleanupStaleProcessing();

            Map<String, Object> result = emailQueueService.processQueue();

            log.info("ProcessEmailQueueJob completed. Result: {}", result);
        } catch (Exception e) {
            log.error("Error executing ProcessEmailQueueJob", e);
            throw new JobExecutionException("Failed to process email queue", e);
        }
    }
}
