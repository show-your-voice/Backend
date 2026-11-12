package com.example.showyourvoice.domain.room.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tbl_room_cell")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RoomCell {

    @EmbeddedId
    private Key id;

    @MapsId("roomId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    public static RoomCell of(Room room, int gridRow, int gridCol) {
        RoomCell cell = new RoomCell();
        cell.id = new Key(room.getId(), gridRow, gridCol);
        cell.room = room;
        return cell;
    }

    @Getter
    @Embeddable
    @EqualsAndHashCode
    @NoArgsConstructor(access = AccessLevel.PROTECTED)
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Key implements Serializable {

        @Column(name = "room_id", nullable = false)
        private Long roomId;

        @Column(name = "grid_row", nullable = false)
        private int gridRow;

        @Column(name = "grid_col", nullable = false)
        private int gridCol;
    }
}
