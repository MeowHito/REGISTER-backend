package com.actionth.membership.dto;

import java.time.OffsetDateTime;

import lombok.Builder;
import lombok.Data;

/** What the back office shows next to the "sync now" button. */
@Data
@Builder
public class EventCalendarImportStatus {
    private boolean enabled;
    private boolean running;
    /** Horizon used by the nightly job (last value chosen in the back office). */
    private int horizonMonths;
    private String source;
    private String sourceUrl;
    /** Last source-side modification time processed; null until the first successful run. */
    private OffsetDateTime watermark;
    private EventCalendarImportResult lastRun;
    /** Live counters of the run in progress; null when idle. */
    private EventCalendarImportResult currentRun;
    /** Imported rows still waiting for admin approval. */
    private long pendingCount;
}
