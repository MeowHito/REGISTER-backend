package com.actionth.membership.exception;

/**
 * Exception thrown when attempting to modify event data that has active participants.
 * This protects registered participants from having their registration details changed.
 */
public class EventModificationException extends RuntimeException {

    private final String field;
    private final String itemName;

    public EventModificationException(String message) {
        super(message);
        this.field = null;
        this.itemName = null;
    }

    public EventModificationException(String message, String field, String itemName) {
        super(message);
        this.field = field;
        this.itemName = itemName;
    }

    public String getField() {
        return field;
    }

    public String getItemName() {
        return itemName;
    }
}
