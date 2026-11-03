package com.example.showyourvoice.domain.room.controller;

import com.example.showyourvoice.domain.room.controller.dto.RoomInfoResponse;
import com.example.showyourvoice.domain.room.service.RoomService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @GetMapping
    public ResponseEntity<List<RoomInfoResponse>> getRooms(@RequestParam(defaultValue = "") String keyword) {
        return ResponseEntity.ok(roomService.getRooms(keyword));
    }
}
