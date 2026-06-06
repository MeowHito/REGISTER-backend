package com.actionth.membership.service;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.actionth.membership.model.dto.JobScheduleDto;
import com.actionth.membership.model.dto.JobScheduleUpdateDto;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for managing Quartz Scheduler configurations
 */
@Slf4j
@Service
public class QuartzSchedulerService {

    @Autowired
    private Scheduler scheduler;

    /**
     * Update job schedule with new cron expression
     * 
     * @param jobName Job name to update
     * @param jobGroup Job group (optional, uses default if not provided)
     * @param newCronExpression New cron expression
     * @param timezone Timezone for the schedule (optional, uses server timezone if not provided)
     * @return JobScheduleUpdateDto with update result
     * @throws SchedulerException if update fails
     */
    public JobScheduleUpdateDto updateJobSchedule(String jobName, String jobGroup, String newCronExpression, String timezone) throws SchedulerException {
        try {
            // Use provided timezone or server default
            TimeZone tz = (timezone != null && !timezone.isEmpty()) 
                    ? TimeZone.getTimeZone(timezone) 
                    : TimeZone.getDefault();
            
            // Create JobKey with group
            // Note: JobKey (name + group) is unique in Quartz - enforced by the framework
            // Quartz prevents duplicate jobs with the same name+group combination
            JobKey jobKey = (jobGroup != null && !jobGroup.isEmpty()) 
                    ? new JobKey(jobName, jobGroup)
                    : new JobKey(jobName);
            
            // Find the trigger for the job using JobKey to get the correct trigger
            // If job doesn't exist, triggers list will be empty
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (triggers.isEmpty()) {
                log.error("No triggers found for job: {} in group: {}", jobName, jobGroup);
                return JobScheduleUpdateDto.builder()
                        .success(false)
                        .message("ไม่พบ Job ที่ต้องการอัปเดต")
                        .build();
            }
            
            // Use the first trigger (typically there's only one)
            Trigger oldTrigger = triggers.get(0);
            TriggerKey triggerKey = oldTrigger.getKey();

            // Validate cron expression using Quartz's own parser before use
            if (!CronExpression.isValidExpression(newCronExpression)) {
                log.error("Invalid Quartz cron expression: {}", newCronExpression);
                return JobScheduleUpdateDto.builder()
                        .success(false)
                        .message("Cron expression '" + newCronExpression + "' is invalid. " +
                                "Remember: when Day-of-Week is specified, Day-of-Month must be '?' (e.g. '0 0 0 ? * MON')")
                        .build();
            }

            // Create new trigger with updated cron expression
            CronScheduleBuilder scheduleBuilder = CronScheduleBuilder.cronSchedule(newCronExpression);
            if (timezone != null && !timezone.isEmpty()) {
                scheduleBuilder.inTimeZone(tz);
            }
            
            Trigger newTrigger = TriggerBuilder.newTrigger()
                    .withIdentity(triggerKey)
                    .withSchedule(scheduleBuilder)
                    .build();

            // Reschedule the job
            Date nextFireTime = scheduler.rescheduleJob(triggerKey, newTrigger);
            
            log.info("Successfully updated schedule for job: {} with cron: {} in timezone: {}", 
                    jobName, newCronExpression, tz.getID());
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(tz);
            
            return JobScheduleUpdateDto.builder()
                    .success(true)
                    .message("อัปเดตตารางเวลาสำเร็จ")
                    .jobName(jobName)
                    .cronExpression(newCronExpression)
                    .timezone(tz.getID())
                    .nextFireTime(nextFireTime != null ? sdf.format(nextFireTime) : null)
                    .build();
            
        } catch (Exception e) {
            log.error("Failed to update job schedule for: {}", jobName, e);
            throw e;
        }
    }

    /**
     * Get current schedule information for a job
     * 
     * @param jobName Job name
     * @param jobGroup Job group (optional, uses default if not provided)
     * @return JobScheduleDto with schedule information
     * @throws SchedulerException if retrieval fails
     */
    public JobScheduleDto getCurrentSchedule(String jobName, String jobGroup) throws SchedulerException {
        // Create JobKey with group
        JobKey jobKey = (jobGroup != null && !jobGroup.isEmpty()) 
                ? new JobKey(jobName, jobGroup)
                : new JobKey(jobName);
        
        // Get triggers for this job
        List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
        if (triggers.isEmpty()) {
            return null;
        }
        
        // Use the first trigger
        Trigger trigger = triggers.get(0);
        if (!(trigger instanceof CronTrigger cronTrigger)) {
            return null;
        }
        
        TimeZone tz = cronTrigger.getTimeZone() != null ? cronTrigger.getTimeZone() : TimeZone.getDefault();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(tz);
        
        Date nextFireTime = cronTrigger.getNextFireTime();
        Date previousFireTime = cronTrigger.getPreviousFireTime();
        
        return JobScheduleDto.builder()
                .jobName(jobName)
                .cronExpression(cronTrigger.getCronExpression())
                .timezone(tz.getID())
                .nextFireTime(nextFireTime != null ? sdf.format(nextFireTime) : null)
                .previousFireTime(previousFireTime != null ? sdf.format(previousFireTime) : null)
                .build();
    }
}
