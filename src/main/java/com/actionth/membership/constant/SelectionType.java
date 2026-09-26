package com.actionth.membership.constant;

/**
 * Answer kinds for an event's extra questions. SINGLE / MULTIPLE pick from options; TEXT is a
 * free-text answer; RATING is a 1-5 score; IMAGE is an uploaded picture (stored as its public URL).
 */
public enum SelectionType {
    SINGLE, MULTIPLE, TEXT, RATING, IMAGE
}
