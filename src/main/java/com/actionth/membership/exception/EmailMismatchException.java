package com.actionth.membership.exception;

import lombok.Getter;

@Getter
public class EmailMismatchException extends RuntimeException {

    private final String invitedEmail;

    public EmailMismatchException(String invitedEmail) {
        super("Email mismatch: this invitation was sent to " + invitedEmail);
        this.invitedEmail = invitedEmail;
    }
}
