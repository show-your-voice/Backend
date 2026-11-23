package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class EmailNotVerifiedException extends BusinessException {

    public EmailNotVerifiedException() {
        super(ErrorCode.EMAIL_NOT_VERIFIED, "email");
    }
}
