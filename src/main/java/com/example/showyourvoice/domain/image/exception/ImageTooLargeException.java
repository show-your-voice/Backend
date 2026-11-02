package com.example.showyourvoice.domain.image.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class ImageTooLargeException extends BusinessException {

    public ImageTooLargeException() {
        super(ErrorCode.IMAGE_TOO_LARGE, "file");
    }
}
