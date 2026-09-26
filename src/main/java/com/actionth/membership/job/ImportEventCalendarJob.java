package com.actionth.membership.job;

import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.actionth.membership.dto.EventCalendarImportResult;
import com.actionth.membership.service.EventCalendarImportService;

import lombok.extern.slf4j.Slf4j;

/**
 * Quartz job that pulls upcoming races from the external calendar site into the pending
 * event-calendar queue. Runs nightly and on demand from the back office ("sync now").
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class ImportEventCalendarJob implements Job {

    public static final String JOB_NAME = "importEventCalendarJob";
    public static final String DATA_HORIZON = "horizonMonths";
    public static final String DATA_CLEAR_FIRST = "clearFirst";

    @Autowired
    private EventCalendarImportService eventCalendarImportService;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        try {
            var data = context.getMergedJobDataMap();
            Integer horizon = data.containsKey(DATA_HORIZON) ? data.getInt(DATA_HORIZON) : null;
            boolean clearFirst = data.containsKey(DATA_CLEAR_FIRST) && data.getBoolean(DATA_CLEAR_FIRST);
            EventCalendarImportResult result = eventCalendarImportService.sync(horizon, clearFirst);
            if (result.getError() != null) {
                throw new JobExecutionException("EventCalendar import failed: " + result.getError());
            }
        } catch (IllegalStateException e) {
            // disabled or already running — nothing to retry
            log.info("ImportEventCalendarJob skipped: {}", e.getMessage());
        }
    }
}
