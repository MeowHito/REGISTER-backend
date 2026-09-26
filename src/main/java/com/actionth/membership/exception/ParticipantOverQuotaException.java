package com.actionth.membership.exception;

/**
 * A back-office edit would move a runner into a distance whose quota is full. Not an error:
 * the controller hands the Thai message back so the user can confirm and resend.
 */
public class ParticipantOverQuotaException extends RuntimeException {
    public ParticipantOverQuotaException(String message) {
        super(message);
    }
}
