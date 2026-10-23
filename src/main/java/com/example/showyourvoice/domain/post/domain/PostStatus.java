package com.example.showyourvoice.domain.post.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostStatus {
    UNRESOLVED("미해결"),
    IN_PROGRESS("진행 중"),
    RESOLVED("해결 완료");

    private final String label;
}
