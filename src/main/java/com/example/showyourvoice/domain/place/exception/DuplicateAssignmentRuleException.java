package com.example.showyourvoice.domain.place.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class DuplicateAssignmentRuleException extends BusinessException {

    public DuplicateAssignmentRuleException() {
        super(ErrorCode.DUPLICATE_ASSIGNMENT_RULE);
    }
}
