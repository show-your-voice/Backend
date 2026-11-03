package com.example.showyourvoice.domain.room.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    @EntityGraph(attributePaths = "place")
    List<Room> findAllByNameContainingOrderByNameAscPlaceNameAscIdAsc(String keyword);

    @Override
    @EntityGraph(attributePaths = "place")
    Optional<Room> findById(Long id);
}
