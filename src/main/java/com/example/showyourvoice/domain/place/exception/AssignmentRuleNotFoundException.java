package com.example.showyourvoice.domain.place.exception;

import com.example.showyourvoice.global.error.BusinessException;
import com.example.showyourvoice.global.error.ErrorCode;

public class AssignmentRuleNotFoundException extends BusinessException {

    public AssignmentRuleNotFoundException() {
        super(ErrorCode.ASSIGNMENT_RULE_NOT_FOUND);
    }
}
