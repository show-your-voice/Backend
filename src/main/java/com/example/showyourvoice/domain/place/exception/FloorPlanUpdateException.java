package com.example.showyourvoice.domain.place.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class FloorPlanUpdateException extends BusinessException {

    public FloorPlanUpdateException(ErrorCode errorCode) {
        super(errorCode);
    }
}
