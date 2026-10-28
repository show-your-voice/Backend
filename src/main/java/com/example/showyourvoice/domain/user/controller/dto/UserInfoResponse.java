package com.example.showyourvoice.domain.user.controller.dto;

import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;

public record UserInfoResponse(
        Long id,
        String loginId,
        String email,
        String name,
        Role role
) {

    public static UserInfoResponse from(User user) {
        return new UserInfoResponse(user.getId(), user.getLoginId(), user.getEmail(), user.getName(), user.getRole());
    }
}
