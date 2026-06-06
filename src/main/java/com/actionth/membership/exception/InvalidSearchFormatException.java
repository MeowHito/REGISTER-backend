package com.actionth.membership.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSearchFormatException extends RuntimeException {

    public InvalidSearchFormatException(String message) {
        super(message);
    }

    public InvalidSearchFormatException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidSearchFormatException(String field, String value, String expectedFormat) {
        super(String.format("Invalid format for field '%s'. Value '%s' does not match expected format: %s", field, value, expectedFormat));
    }
}
