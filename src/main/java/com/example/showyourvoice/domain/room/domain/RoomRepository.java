package com.example.showyourvoice.domain.room.domain;

import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<Room, Long> {

    List<Room> findAllByPlaceIdOrderByIdAsc(Long placeId);

    @EntityGraph(attributePaths = "place")
    List<Room> findAllByNameContainingOrderByNameAscPlaceNameAscIdAsc(String keyword);

    @Override
    @EntityGraph(attributePaths = "place")
    Optional<Room> findById(Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Room r where r.id = :id")
    Optional<Room> findByIdForUpdate(@Param("id") Long id);
}
