package com.actionth.membership.config;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.actionth.membership.job.ProcessEmailQueueJob;
import com.actionth.membership.job.ResendFailedEmailsJob;
import com.actionth.membership.job.UpdateOverduePaymentsJob;
import com.actionth.membership.listener.JobExecutionLogger;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz Scheduler Configuration
 * Configures scheduled jobs for the application
 */
@Slf4j
@Configuration
public class QuartzConfig {

    @Value("${app.env}")
    private String appEnv;

    /**
     * JobDetail for updating overdue payments
     */
    @Bean
    public JobDetail updateOverduePaymentsJobDetail() {
        return JobBuilder.newJob(UpdateOverduePaymentsJob.class)
                .withIdentity("updateOverduePaymentsJob")
                .withDescription("Update overdue payments and release coupons from failed orders")
                .storeDurably()
                .build();
    }

    /**
     * Trigger for updating overdue payments
     * Production: Runs every day at midnight (00:00:00) Thailand time (Asia/Bangkok)
     * Non-production: Can be configured for testing
     */
    @Bean
    public Trigger updateOverduePaymentsTrigger() {
        // Daily at midnight
        // For testing every minute: "0 * * * * ?"
        String cronExpression = "0 0 0 * * ?";
        
        return TriggerBuilder.newTrigger()
                .forJob(updateOverduePaymentsJobDetail())
                .withIdentity("updateOverduePaymentsTrigger")
                .withDescription("Trigger for updating overdue payments")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }

    /**
     * JobDetail for resending failed emails with daily limit exceeded error
     */
    @Bean
    public JobDetail resendFailedEmailsJobDetail() {
        return JobBuilder.newJob(ResendFailedEmailsJob.class)
                .withIdentity("resendFailedEmailsJob")
                .withDescription("Resend failed emails with daily sending limit exceeded error")
                .storeDurably()
                .build();
    }

    /**
     * Trigger for resending failed emails
     * Production: Runs every day at 2:00 AM Thailand time (Asia/Bangkok)
     * Non-production: Can be configured for testing
     */
    @Bean
    public Trigger resendFailedEmailsTrigger() {
        // Daily at 2:00 AM (after the daily sending limit might reset)
        // For testing every 5 minutes: "0 */5 * * * ?"
        String cronExpression = "0 0 2 * * ?";
        
        return TriggerBuilder.newTrigger()
                .forJob(resendFailedEmailsJobDetail())
                .withIdentity("resendFailedEmailsTrigger")
                .withDescription("Trigger for resending failed emails")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }

    /**
     * Register global job listener for execution logging
     */
    @Bean
    public JobExecutionLogger configureJobListener(Scheduler scheduler, JobExecutionLogger jobExecutionLogger) throws SchedulerException {
        scheduler.getListenerManager().addJobListener(jobExecutionLogger);
        log.info("JobExecutionLogger registered with Quartz Scheduler");
        return jobExecutionLogger;
    }

    /**
     * JobDetail for processing email queue
     */
    @Bean
    public JobDetail processEmailQueueJobDetail() {
        return JobBuilder.newJob(ProcessEmailQueueJob.class)
                .withIdentity("processEmailQueueJob")
                .withDescription("Process pending emails from queue with daily limit")
                .storeDurably()
                .build();
    }

    /**
     * Trigger for processing email queue
     * Runs every 30 minutes to gradually send queued emails
     */
    @Bean
    public Trigger processEmailQueueTrigger() {
        String cronExpression = "0 */30 * * * ?";

        return TriggerBuilder.newTrigger()
                .forJob(processEmailQueueJobDetail())
                .withIdentity("processEmailQueueTrigger")
                .withDescription("Trigger for processing email queue")
                .withSchedule(CronScheduleBuilder.cronSchedule(cronExpression))
                .build();
    }
}
