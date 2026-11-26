package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class InvalidPostStatusTransitionException extends BusinessException {

    public InvalidPostStatusTransitionException() {
        super(ErrorCode.INVALID_POST_STATUS_TRANSITION);
    }
}
