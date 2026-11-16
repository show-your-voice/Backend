package com.example.showyourvoice.domain.user.controller.dto;

import com.example.showyourvoice.domain.user.domain.User;

public record UserNameResponse(String name) {

    public static UserNameResponse from(User user) {
        return new UserNameResponse(user.getName());
    }
}
