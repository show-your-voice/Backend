package com.example.showyourvoice.domain.post.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum PostCategory {
    FACILITY("시설 고장"),
    NETWORK("네트워크"),
    CLEANING("청소"),
    OTHER("기타");

    private final String label;
}
