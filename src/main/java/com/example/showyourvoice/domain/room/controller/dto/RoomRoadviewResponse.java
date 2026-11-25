package com.example.showyourvoice.domain.room.controller.dto;

import com.example.showyourvoice.domain.room.domain.Room;

public record RoomRoadviewResponse(Long roomId, String roomName, String imageUrl) {

    public static RoomRoadviewResponse from(Room room) {
        return new RoomRoadviewResponse(room.getId(), room.getName(), room.getRoadviewImageUrl());
    }
}
