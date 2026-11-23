package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class EmailVerificationNotFoundException extends BusinessException {

    public EmailVerificationNotFoundException() {
        super(ErrorCode.EMAIL_VERIFICATION_NOT_FOUND, "email");
    }
}
