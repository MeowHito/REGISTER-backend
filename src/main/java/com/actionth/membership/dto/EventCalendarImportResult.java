package com.actionth.membership.dto;

import java.time.OffsetDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class EventCalendarImportResult {

    /** MONTHLY: every month page from this month to the horizon is read in full. */
    private String mode;
    private OffsetDateTime startedAt;
    private OffsetDateTime finishedAt;
    /** Months ahead this run kept. */
    private int horizonMonths;
    /** Previously imported rows deleted before the scan (clearFirst). */
    private int cleared;
    /** Races in the window found on the month pages; 0 while the pages are still being read. */
    private int total;
    /** Races saved (or found unchanged) so far. */
    private int listed;
    private int created;
    private int updated;
    /** Listed races outside the window (started in an earlier month). */
    private int skipped;
    /** Month pages that could not be read, plus races that could not be saved. */
    private int failed;
    /** Rows of this source in the window that the source no longer lists, deleted at the end. */
    private int removed;
    /** Short error text when the run aborted (null on success). */
    private String error;
    /** An admin stopped the run early; what was imported so far is kept. */
    private boolean stopped;
}
