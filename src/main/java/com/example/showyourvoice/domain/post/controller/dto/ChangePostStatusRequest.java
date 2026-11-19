package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.post.domain.PostStatus;
import jakarta.validation.constraints.NotNull;

public record ChangePostStatusRequest(@NotNull PostStatus status) {
}
