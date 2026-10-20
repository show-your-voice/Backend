package com.example.showyourvoice.domain.user.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException() {
        super(ErrorCode.DUPLICATE_EMAIL);
    }
}
