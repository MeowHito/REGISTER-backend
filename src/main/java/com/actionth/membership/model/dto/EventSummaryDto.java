package com.actionth.membership.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Counts behind the stat cards on the back-office event list. */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSummaryDto {
    private long total;
    /** Not a draft — open to the public. */
    private long published;
    /** Registration window already ended. */
    private long closed;
    /** Distinct provinces the events are held in. */
    private long provinces;
}
