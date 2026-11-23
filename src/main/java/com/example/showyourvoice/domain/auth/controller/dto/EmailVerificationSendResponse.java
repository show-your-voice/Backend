package com.example.showyourvoice.domain.auth.controller.dto;

import com.example.showyourvoice.domain.auth.domain.EmailVerification;
import java.time.LocalDateTime;

public record EmailVerificationSendResponse(
        LocalDateTime expiresAt,
        long expiresInSeconds,
        int remainingAttempts
) {

    public static EmailVerificationSendResponse from(EmailVerification verification) {
        return new EmailVerificationSendResponse(verification.getExpiresAt(),
                EmailVerification.CODE_TTL.toSeconds(), verification.getRemainingAttempts());
    }
}
