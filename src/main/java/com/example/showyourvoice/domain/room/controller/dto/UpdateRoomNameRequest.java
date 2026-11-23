package com.example.showyourvoice.domain.room.controller.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateRoomNameRequest(@NotBlank @Size(max = 50) String name) {

    public UpdateRoomNameRequest {
        if (name != null) {
            name = name.strip();
        }
    }
}
