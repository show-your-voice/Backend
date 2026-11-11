package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class InvalidPostCoAssigneeException extends BusinessException {

    public InvalidPostCoAssigneeException() {
        super(ErrorCode.INVALID_POST_CO_ASSIGNEE);
    }
}
