package com.example.showyourvoice.domain.place.service;

import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse;
import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse.Cell;
import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse.RoomPlan;
import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.place.exception.PlaceNotFoundException;
import com.example.showyourvoice.domain.room.domain.RoomCellRepository;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlaceService {

    private final PlaceRepository placeRepository;
    private final RoomRepository roomRepository;
    private final RoomCellRepository roomCellRepository;

    public List<PlaceInfoResponse> getPlaces() {
        return placeRepository.findAllByOrderByNameAscIdAsc().stream()
                .map(PlaceInfoResponse::from)
                .toList();
    }

    public PlaceFloorPlanResponse getFloorPlan(Long placeId) {
        Place place = placeRepository.findById(placeId).orElseThrow(PlaceNotFoundException::new);
        Integer rows = place.getGridRows();
        Integer cols = place.getGridCols();
        Map<Long, List<Cell>> cellsByRoom = new HashMap<>();

        if (rows != null && cols != null) {
            roomCellRepository.findByPlaceId(placeId).forEach(cell -> {
                var key = cell.getId();
                cellsByRoom.computeIfAbsent(key.getRoomId(), id -> new ArrayList<>())
                        .add(new Cell(key.getGridRow(), key.getGridCol()));
            });
        }

        List<RoomPlan> rooms = roomRepository.findAllByPlaceIdOrderByIdAsc(placeId).stream()
                .map(room -> new RoomPlan(room.getId(), room.getName(), cellsByRoom.getOrDefault(room.getId(), List.of())))
                .toList();
        return new PlaceFloorPlanResponse(rows, cols, rooms);
    }
}
