package com.example.showyourvoice.domain.image.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class ImageNotFoundException extends BusinessException {

    public ImageNotFoundException() {
        super(ErrorCode.IMAGE_NOT_FOUND, "imageUrls");
    }
}
