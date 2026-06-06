package com.actionth.membership.job;

import java.time.OffsetDateTime;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.actionth.membership.service.EmailLogService;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Job to automatically resend failed emails with daily limit exceeded errors.
 * Targets registration confirmation and payment emails that failed due to Gmail daily sending limits.
 * Runs daily at a configured time.
 */
@Slf4j
@Component
public class ResendFailedEmailsJob implements Job {

    @Autowired
    private EmailLogService emailLogService;

    @Override
    @Transactional
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            log.info("Starting ResendFailedEmailsJob at {}", OffsetDateTime.now());
            
            int processedCount = emailLogService.resendFailedEmailsWithDailyLimitExceeded();
            
            log.info("ResendFailedEmailsJob completed successfully. Processed {} emails.", processedCount);
        } catch (Exception e) {
            log.error("Error executing ResendFailedEmailsJob", e);
            throw new JobExecutionException("Failed to resend failed emails", e);
        }
    }
}
