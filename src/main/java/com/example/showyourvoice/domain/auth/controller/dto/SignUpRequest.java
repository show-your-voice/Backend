package com.example.showyourvoice.domain.auth.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignUpRequest(
        @NotBlank(message = "아이디를 입력해주세요.")
        @Pattern(regexp = "^[a-z0-9]{4,20}$", message = "아이디는 영문 소문자와 숫자 4~20자여야 합니다.")
        String loginId,

        @NotBlank(message = "이메일을 입력해주세요.")
        @Size(max = 255, message = "이메일은 255자 이하여야 합니다.")
        @Pattern(regexp = "^[A-Za-z0-9._%+-]+@dsm\\.hs\\.kr$", message = "@dsm.hs.kr 이메일만 사용할 수 있습니다.")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 8, max = 50, message = "비밀번호는 8자 이상 50자 이하여야 합니다.")
        String password,

        @NotBlank(message = "이름을 입력해주세요.")
        @Size(min = 2, max = 50, message = "이름은 2자 이상 50자 이하여야 합니다.")
        String name
) {
}
