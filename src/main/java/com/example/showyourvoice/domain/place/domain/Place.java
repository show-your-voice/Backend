package com.example.showyourvoice.domain.place.domain;

import com.example.showyourvoice.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tbl_place")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String buildingName;

    @Column(nullable = false)
    private Integer floor;

    @Column(nullable = false, length = 100)
    private String name;

    @Builder
    private Place(String buildingName, Integer floor, String name) {
        this.buildingName = buildingName;
        this.floor = floor;
        this.name = name;
    }
}
