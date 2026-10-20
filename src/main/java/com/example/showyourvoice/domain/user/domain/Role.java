package com.example.showyourvoice.domain.user.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum Role {

    USER("ROLE_USER"),
    TEACHER("ROLE_TEACHER");

    private final String authority;
}
