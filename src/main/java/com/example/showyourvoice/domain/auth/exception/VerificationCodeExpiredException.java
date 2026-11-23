package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class VerificationCodeExpiredException extends BusinessException {

    public VerificationCodeExpiredException() {
        super(ErrorCode.VERIFICATION_CODE_EXPIRED, "code");
    }
}
