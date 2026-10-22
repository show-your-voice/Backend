package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CreatePostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 100, message = "제목은 100자 이하여야 합니다.")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Size(max = 5000, message = "내용은 5000자 이하여야 합니다.")
        String content,

        @NotNull(message = "카테고리를 선택해주세요.")
        PostCategory category,

        @NotNull(message = "장소를 선택해주세요.")
        @Positive(message = "장소 번호는 양수여야 합니다.")
        Long placeId,

        @Size(max = 1000, message = "개선방향은 1000자 이하여야 합니다.")
        String improvement
) {
}
