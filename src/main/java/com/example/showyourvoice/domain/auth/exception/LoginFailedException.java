package com.example.showyourvoice.domain.auth.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class LoginFailedException extends BusinessException {

    public LoginFailedException() {
        super(ErrorCode.LOGIN_FAILED);
    }
}
