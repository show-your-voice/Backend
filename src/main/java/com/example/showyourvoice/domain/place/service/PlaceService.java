package com.example.showyourvoice.domain.place.service;

import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse;
import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse.Cell;
import com.example.showyourvoice.domain.place.controller.dto.PlaceFloorPlanResponse.RoomPlan;
import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.place.controller.dto.UpdatePlaceFloorPlanRequest;
import com.example.showyourvoice.domain.place.controller.dto.UpdatePlaceFloorPlanRequest.RoomSelection;
import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.place.exception.FloorPlanUpdateException;
import com.example.showyourvoice.domain.place.exception.PlaceNotFoundException;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.room.domain.RoomCell;
import com.example.showyourvoice.domain.room.domain.RoomCellRepository;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import com.example.showyourvoice.domain.room.exception.RoomNotFoundException;
import com.example.showyourvoice.global.error.ErrorCode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
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

    @Transactional
    public PlaceFloorPlanResponse updateFloorPlan(Long placeId, UpdatePlaceFloorPlanRequest request) {
        RoomSelection first = request.firstRoom();
        RoomSelection second = request.secondRoom();
        if (first.roomId().equals(second.roomId())) {
            throw new FloorPlanUpdateException(ErrorCode.INVALID_INPUT_VALUE);
        }

        Place place = placeRepository.findByIdForUpdate(placeId).orElseThrow(PlaceNotFoundException::new);
        List<Room> rooms = roomRepository.findAllByPlaceIdOrderByIdAsc(placeId);
        Room firstRoom = findRoom(rooms, first.roomId());
        Room secondRoom = findRoom(rooms, second.roomId());
        Integer rows = place.getGridRows();
        Integer cols = place.getGridCols();
        if (rows == null || cols == null || rows <= 0 || cols <= 0) {
            throw new FloorPlanUpdateException(ErrorCode.INVALID_FLOOR_PLAN_LAYOUT);
        }

        List<RoomCell> firstMappings = new ArrayList<>();
        List<RoomCell> secondMappings = new ArrayList<>();
        Set<Cell> occupied = new HashSet<>();
        for (RoomCell mapping : roomCellRepository.findByPlaceId(placeId)) {
            Long roomId = mapping.getId().getRoomId();
            if (roomId.equals(first.roomId())) {
                firstMappings.add(mapping);
            } else if (roomId.equals(second.roomId())) {
                secondMappings.add(mapping);
            } else {
                occupied.add(toCell(mapping));
            }
        }

        Set<Cell> firstCells = firstMappings.stream().map(PlaceService::toCell).collect(Collectors.toSet());
        Set<Cell> secondCells = secondMappings.stream().map(PlaceService::toCell).collect(Collectors.toSet());
        validateExpectedCells(first, firstCells);
        validateExpectedCells(second, secondCells);

        for (Set<Cell> cells : List.of(firstCells, secondCells)) {
            for (Cell cell : cells) {
                if (cell.gridRow() < 0 || cell.gridRow() >= rows || cell.gridCol() < 0 || cell.gridCol() >= cols
                        || !occupied.add(cell)) {
                    throw new FloorPlanUpdateException(ErrorCode.INVALID_FLOOR_PLAN_LAYOUT);
                }
            }
        }
        if (!relativeShape(firstCells).equals(relativeShape(secondCells))) {
            throw new FloorPlanUpdateException(ErrorCode.FLOOR_PLAN_SHAPE_MISMATCH);
        }

        roomCellRepository.deleteAll(firstMappings);
        roomCellRepository.deleteAll(secondMappings);
        roomCellRepository.flush();
        roomCellRepository.saveAll(secondCells.stream()
                .map(cell -> RoomCell.of(firstRoom, cell.gridRow(), cell.gridCol())).toList());
        roomCellRepository.saveAll(firstCells.stream()
                .map(cell -> RoomCell.of(secondRoom, cell.gridRow(), cell.gridCol())).toList());
        roomCellRepository.flush();
        return getFloorPlan(placeId);
    }

    private static Room findRoom(List<Room> rooms, Long roomId) {
        return rooms.stream().filter(room -> room.getId().equals(roomId)).findFirst()
                .orElseThrow(RoomNotFoundException::new);
    }

    private static Cell toCell(RoomCell mapping) {
        return new Cell(mapping.getId().getGridRow(), mapping.getId().getGridCol());
    }

    private static void validateExpectedCells(RoomSelection selection, Set<Cell> actual) {
        if (actual.isEmpty()) {
            throw new FloorPlanUpdateException(ErrorCode.INVALID_FLOOR_PLAN_LAYOUT);
        }
        Set<Cell> expected = selection.expectedCells().stream()
                .map(cell -> new Cell(cell.gridRow(), cell.gridCol())).collect(Collectors.toSet());
        if (expected.size() != selection.expectedCells().size()) {
            throw new FloorPlanUpdateException(ErrorCode.INVALID_INPUT_VALUE);
        }
        if (!actual.equals(expected)) {
            throw new FloorPlanUpdateException(ErrorCode.FLOOR_PLAN_CHANGED);
        }
    }

    private static Set<Cell> relativeShape(Set<Cell> cells) {
        int minRow = cells.stream().mapToInt(Cell::gridRow).min().orElseThrow();
        int minCol = cells.stream().mapToInt(Cell::gridCol).min().orElseThrow();
        return cells.stream().map(cell -> new Cell(cell.gridRow() - minRow, cell.gridCol() - minCol))
                .collect(Collectors.toSet());
    }
}
