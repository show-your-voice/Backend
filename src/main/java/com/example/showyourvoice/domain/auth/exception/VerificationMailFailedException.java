package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class VerificationMailFailedException extends BusinessException {

    public VerificationMailFailedException() {
        super(ErrorCode.VERIFICATION_MAIL_FAILED, "email");
    }
}
