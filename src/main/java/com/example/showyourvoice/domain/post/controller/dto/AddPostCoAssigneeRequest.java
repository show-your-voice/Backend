package com.example.showyourvoice.domain.post.controller.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddPostCoAssigneeRequest(
        @NotNull(message = "공동 담당자를 선택해주세요.")
        @Positive(message = "담당자 번호는 양수여야 합니다.")
        Long managerId
) {
}
