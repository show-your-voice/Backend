package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class DuplicatePostCoAssigneeException extends BusinessException {

    public DuplicatePostCoAssigneeException() {
        super(ErrorCode.DUPLICATE_POST_CO_ASSIGNEE);
    }
}
