package com.actionth.membership.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Summary of one run of the external event-calendar import. Persisted (compact) in appConfig. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventCalendarImportResult {

    /** BACKFILL (first run, no watermark yet) or INCREMENTAL. */
    private String mode;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    /** Months ahead this run kept. */
    private int horizonMonths;
    /** Previously imported rows deleted before the scan (clearFirst). */
    private int cleared;
    /** Events the source listing matched in total (from X-WP-Total); 0 until known. */
    private int total;
    /** Events processed from the listing so far. */
    private int listed;
    private int created;
    private int updated;
    /** Listed events outside the today..today+horizon window that we don't hold yet. */
    private int skipped;
    private int failed;
    /** Short error text when the run aborted (null on success). */
    private String error;
    /** An admin stopped the run early; what was imported so far is kept. */
    private boolean stopped;
}
