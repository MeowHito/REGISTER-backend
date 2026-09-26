package com.actionth.membership.dto;

import lombok.Data;

/** Options for a manual "sync now" from the back office. */
@Data
public class EventCalendarImportRequest {
    /** Keep races dated today .. today + this many months (1-36). Null = last used / default. */
    private Integer horizonMonths;
    /** Delete every previously imported row (never manual submissions) before re-scanning. */
    private boolean clearFirst;
}
