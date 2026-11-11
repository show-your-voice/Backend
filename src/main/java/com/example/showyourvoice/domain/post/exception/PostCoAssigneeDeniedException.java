package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class PostCoAssigneeDeniedException extends BusinessException {

    public PostCoAssigneeDeniedException() {
        super(ErrorCode.POST_CO_ASSIGNEE_DENIED);
    }
}
