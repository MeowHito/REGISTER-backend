package com.actionth.membership.constant;

public final class NotificationType {

    private NotificationType() {
    }

    /** Admin to-dos. */
    public static final String ORGANIZER_PENDING = "ORGANIZER_PENDING";
    public static final String HELP_REQUEST = "HELP_REQUEST";
    public static final String ANNOUNCEMENT_SUBMITTED = "ANNOUNCEMENT_SUBMITTED";
    public static final String EVENT_CALENDAR_SUBMITTED = "EVENT_CALENDAR_SUBMITTED";

    /** Orders. */
    public static final String ORDER_REVIEW = "ORDER_REVIEW";
    public static final String ORDER_PAID = "ORDER_PAID";

    /** Event collaboration. */
    public static final String EVENT_INVITED = "EVENT_INVITED";
    public static final String EVENT_PERMISSION_GRANTED = "EVENT_PERMISSION_GRANTED";
}
