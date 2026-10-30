package com.example.showyourvoice.domain.user.controller.dto;

import com.example.showyourvoice.domain.user.domain.User;
import java.util.List;

public record ManagerInfoResponse(Long id, String name, String email, List<String> locations) {

    public static ManagerInfoResponse of(User user, List<String> locations) {
        return new ManagerInfoResponse(user.getId(), user.getName(), user.getEmail(), locations);
    }
}
