package com.example.showyourvoice.global.error;

import java.util.List;

public record LimitedAttemptErrorResponse(
        String code,
        String message,
        List<ErrorResponse.FieldError> errors,
        int remainingAttempts
) {

    public static LimitedAttemptErrorResponse of(LimitedAttemptException e) {
        ErrorResponse base = ErrorResponse.of(e.getErrorCode(), e.getField());
        return new LimitedAttemptErrorResponse(base.code(), base.message(), base.errors(), e.getRemainingAttempts());
    }
}
