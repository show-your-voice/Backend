package com.example.showyourvoice.domain.room.domain;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomCellRepository extends JpaRepository<RoomCell, RoomCell.Key> {

    @Query("""
            select c from RoomCell c
            where c.room.place.id = :placeId
            order by c.id.gridRow, c.id.gridCol, c.id.roomId
            """)
    List<RoomCell> findByPlaceId(@Param("placeId") Long placeId);
}
