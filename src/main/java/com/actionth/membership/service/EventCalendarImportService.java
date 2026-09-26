package com.actionth.membership.service;

import com.actionth.membership.dto.EventCalendarImportResult;
import com.actionth.membership.dto.EventCalendarImportStatus;

/**
 * Pulls running events from an external calendar site (joggingandrunning.com, a WordPress +
 * EventON site with an open REST API) into {@code eventCalendar} as pending submissions.
 */
public interface EventCalendarImportService {

    /**
     * Runs one sync: the first run backfills upcoming events, later runs only fetch events the
     * source modified since the last watermark. Safe to call repeatedly; a second concurrent
     * call is rejected with {@link IllegalStateException}.
     */
    EventCalendarImportResult sync();

    /**
     * Same as {@link #sync()} but with explicit options: a horizon in months (1-36, remembered for
     * later runs) and whether to wipe previously imported rows first (which also resets the
     * watermark so the source is scanned from scratch).
     */
    EventCalendarImportResult sync(Integer horizonMonths, boolean clearFirst);

    /** Deletes every imported row (manual submissions are untouched) and resets the watermark. */
    long clearImported();

    boolean isRunning();

    EventCalendarImportStatus getStatus();
}
