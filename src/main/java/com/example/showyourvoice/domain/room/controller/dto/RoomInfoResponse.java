package com.example.showyourvoice.domain.room.controller.dto;

import com.example.showyourvoice.domain.room.domain.Room;

public record RoomInfoResponse(Long id, String name, Long placeId, String placeName) {

    public static RoomInfoResponse from(Room room) {
        return new RoomInfoResponse(room.getId(), room.getName(), room.getPlace().getId(), room.getPlace().getName());
    }
}
