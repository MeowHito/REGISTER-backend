package com.actionth.membership.service;

import com.actionth.membership.dto.EventCalendarImportResult;
import com.actionth.membership.dto.EventCalendarImportStatus;

/**
 * Pulls running events from an external calendar site (patrunning.com's monthly calendar pages)
 * into {@code eventCalendar}, live on the public calendar straight away.
 */
public interface EventCalendarImportService {

    /**
     * Runs one sync: reads every month from this one to the horizon and upserts what it lists.
     * Safe to call repeatedly; a second concurrent call is rejected with {@link IllegalStateException}.
     */
    EventCalendarImportResult sync();

    /**
     * Same as {@link #sync()} but with explicit options: a horizon in months (1-36, remembered for
     * later runs) and whether to wipe previously imported rows first.
     */
    EventCalendarImportResult sync(Integer horizonMonths, boolean clearFirst);

    /** Deletes every imported row, from any source (manual submissions are untouched). */
    long clearImported();

    boolean isRunning();

    /** Asks the run in progress to stop after the item it is on; no-op when idle. */
    boolean requestStop();

    EventCalendarImportStatus getStatus();
}
