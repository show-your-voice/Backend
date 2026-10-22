package com.example.showyourvoice.domain.place.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class PlaceNotFoundException extends BusinessException {

    public PlaceNotFoundException() {
        super(ErrorCode.PLACE_NOT_FOUND, "placeId");
    }
}
