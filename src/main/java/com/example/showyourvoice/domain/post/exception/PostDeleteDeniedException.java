package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class PostDeleteDeniedException extends BusinessException {

    public PostDeleteDeniedException() {
        super(ErrorCode.POST_DELETE_DENIED);
    }
}
