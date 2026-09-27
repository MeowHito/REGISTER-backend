package com.actionth.membership.dto;

import java.util.List;

import lombok.Data;

/** Approve / reject / delete / mark-as-Major many calendar entries at once from the back-office list. */
@Data
public class EventCalendarBulkRequest {
    /** Entry uuids. */
    private List<String> ids;
    /** true = approve, false = reject. Ignored by bulk delete. */
    private Boolean isApproved;
    private String rejectReason;
    /** Used by bulk-major only. */
    private Boolean isMajor;
}
