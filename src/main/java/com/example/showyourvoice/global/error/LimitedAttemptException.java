package com.example.showyourvoice.global.error;

import lombok.Getter;

// 기회가 정해진 요청이 실패했을 때, 남은 기회를 응답에 함께 담기 위한 예외.
@Getter
public class LimitedAttemptException extends BusinessException {

    private final int remainingAttempts;

    public LimitedAttemptException(ErrorCode errorCode, String field, int remainingAttempts) {
        super(errorCode, field);
        this.remainingAttempts = remainingAttempts;
    }
}
