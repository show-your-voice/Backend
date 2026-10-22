package com.example.showyourvoice.domain.place.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlaceTeacherAssignmentRepository extends JpaRepository<PlaceTeacherAssignment, Long> {

    @EntityGraph(attributePaths = "teacher")
    Optional<PlaceTeacherAssignment> findByPlaceId(Long placeId);
}
