package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

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

        @NotNull(message = "X 좌표를 입력해주세요.")
        @DecimalMin(value = "0", message = "X 좌표는 0 이상 1 이하여야 합니다.")
        @DecimalMax(value = "1", message = "X 좌표는 0 이상 1 이하여야 합니다.")
        @Digits(integer = 1, fraction = 4, message = "X 좌표는 소수점 넷째 자리까지 입력할 수 있습니다.")
        BigDecimal posX,

        @NotNull(message = "Y 좌표를 입력해주세요.")
        @DecimalMin(value = "0", message = "Y 좌표는 0 이상 1 이하여야 합니다.")
        @DecimalMax(value = "1", message = "Y 좌표는 0 이상 1 이하여야 합니다.")
        @Digits(integer = 1, fraction = 4, message = "Y 좌표는 소수점 넷째 자리까지 입력할 수 있습니다.")
        BigDecimal posY,

        @Size(max = 1000, message = "개선방향은 1000자 이하여야 합니다.")
        String improvement
) {
}
