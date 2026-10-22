package com.example.showyourvoice.domain.place.service;

import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;

    public List<PlaceInfoResponse> getPlaces() {
        return placeRepository.findAllByOrderByBuildingNameAscFloorAscNameAscIdAsc().stream()
                .map(PlaceInfoResponse::from)
                .toList();
    }
}
