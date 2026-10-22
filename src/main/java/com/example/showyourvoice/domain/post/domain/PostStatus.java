package com.example.showyourvoice.domain.post.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostStatus {
    RECEIVED("접수"),
    IN_PROGRESS("처리 중"),
    COMPLETED("처리 완료");

    private final String label;
}
