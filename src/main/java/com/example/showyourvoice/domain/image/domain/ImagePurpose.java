package com.example.showyourvoice.domain.image.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImagePurpose {
    POST("posts"),
    PLACE("places");

    private final String directory;
}
