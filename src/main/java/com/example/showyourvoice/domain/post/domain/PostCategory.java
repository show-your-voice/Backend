package com.example.showyourvoice.domain.post.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostCategory {
    FACILITY("시설"),
    NETWORK("네트워크"),
    ENVIRONMENT("환경·청소"),
    ETC("기타");

    private final String label;
}
