package com.example.showyourvoice.domain.image.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class InvalidImageException extends BusinessException {

    public InvalidImageException() {
        super(ErrorCode.INVALID_IMAGE, "file");
    }
}
