package com.example.showyourvoice.domain.place.controller;

import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse;
import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.place.service.PlaceService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController {

    private final PlaceService placeService;

    @GetMapping
    public ResponseEntity<List<PlaceInfoResponse>> getPlaces() {
        return ResponseEntity.ok(placeService.getPlaces());
    }

    @GetMapping("/{placeId}/floor-plan")
    public ResponseEntity<PlaceFloorPlanResponse> getFloorPlan(@PathVariable Long placeId) {
        return ResponseEntity.ok(placeService.getFloorPlan(placeId));
    }
}
