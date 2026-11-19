package com.example.showyourvoice.domain.post.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class PostStatusChangeDeniedException extends BusinessException {

    public PostStatusChangeDeniedException() {
        super(ErrorCode.POST_STATUS_CHANGE_DENIED);
    }
}
