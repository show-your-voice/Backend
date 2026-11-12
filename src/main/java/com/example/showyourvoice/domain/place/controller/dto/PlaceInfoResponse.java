package com.example.showyourvoice.domain.place.controller.dto;

import com.example.showyourvoice.domain.place.domain.Place;

public record PlaceInfoResponse(Long id, String name, String imageUrl, Integer gridRows, Integer gridCols) {

    public static PlaceInfoResponse from(Place place) {
        return new PlaceInfoResponse(place.getId(), place.getName(), place.getImageUrl(), place.getGridRows(), place.getGridCols());
    }
}
