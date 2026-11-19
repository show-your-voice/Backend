package com.example.showyourvoice.domain.place.controller.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;

public record UpdatePlaceFloorPlanRequest(@NotNull @Valid RoomSelection firstRoom,
                                          @NotNull @Valid RoomSelection secondRoom) {

    public record RoomSelection(@NotNull @Positive Long roomId,
                                @NotEmpty List<@NotNull @Valid ExpectedCell> expectedCells) {
    }

    public record ExpectedCell(@NotNull @PositiveOrZero Integer gridRow,
                               @NotNull @PositiveOrZero Integer gridCol) {
    }
}
