package com.example.showyourvoice.domain.place.controller.dto;

import java.util.List;

public record PlaceFloorPlanResponse(Integer gridRows, Integer gridCols, List<RoomPlan> rooms) {

    public record RoomPlan(Long id, String name, List<Cell> cells) {
    }

    public record Cell(int gridRow, int gridCol) {
    }
}
