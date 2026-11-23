package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class VerificationAttemptsExceededException extends BusinessException {

    public VerificationAttemptsExceededException() {
        super(ErrorCode.VERIFICATION_ATTEMPTS_EXCEEDED, "code");
    }
}
