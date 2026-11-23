package com.example.showyourvoice.domain.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendEmailVerificationRequest(
        @NotBlank(message = "이메일을 입력해주세요.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@dsm\\.hs\\.kr$", message = "@dsm.hs.kr 이메일만 사용할 수 있습니다.")
        String email
) {
}
