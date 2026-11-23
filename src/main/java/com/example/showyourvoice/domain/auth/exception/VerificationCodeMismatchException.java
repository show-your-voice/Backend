package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.ErrorCode;
import com.example.showyourvoice.global.error.LimitedAttemptException;

public class VerificationCodeMismatchException extends LimitedAttemptException {

    public VerificationCodeMismatchException(int remainingAttempts) {
        super(ErrorCode.VERIFICATION_CODE_MISMATCH, "code", remainingAttempts);
    }
}
