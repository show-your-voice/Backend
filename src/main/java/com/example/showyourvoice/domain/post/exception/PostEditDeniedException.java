package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class PostEditDeniedException extends BusinessException {

    public PostEditDeniedException() {
        super(ErrorCode.POST_EDIT_DENIED);
    }
}
