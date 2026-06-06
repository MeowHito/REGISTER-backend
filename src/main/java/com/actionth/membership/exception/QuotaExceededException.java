package com.actionth.membership.exception;

import com.actionth.membership.model.dto.QuotaValidationError;
import lombok.Getter;

@Getter
public class QuotaExceededException extends RuntimeException {
    private final QuotaValidationError quotaValidationError;

    public QuotaExceededException(QuotaValidationError quotaValidationError) {
        super(quotaValidationError.getMessage());
        this.quotaValidationError = quotaValidationError;
    }
}
