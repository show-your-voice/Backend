package com.example.showyourvoice.domain.user.controller.dto;

import com.example.showyourvoice.domain.user.domain.Role;

public record UserSessionResponse(String name, Role role) {
}
