package com.example.showyourvoice.domain.place.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class InvalidTeacherAssignmentException extends BusinessException {

    public InvalidTeacherAssignmentException() {
        super(ErrorCode.INVALID_TEACHER_ASSIGNMENT);
    }
}
