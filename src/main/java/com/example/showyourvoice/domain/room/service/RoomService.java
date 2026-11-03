package com.example.showyourvoice.domain.room.service;

import com.example.showyourvoice.domain.room.controller.dto.RoomInfoResponse;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RoomService {

    private final RoomRepository roomRepository;

    public List<RoomInfoResponse> getRooms(String keyword) {
        return roomRepository.findAllByNameContainingOrderByNameAscPlaceNameAscIdAsc(keyword.strip()).stream()
                .map(RoomInfoResponse::from)
                .toList();
    }
}
