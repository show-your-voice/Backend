package com.example.showyourvoice.domain.image.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class ImageUploadFailedException extends BusinessException {

    public ImageUploadFailedException() {
        super(ErrorCode.IMAGE_UPLOAD_FAILED);
    }
}
