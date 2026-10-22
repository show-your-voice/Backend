package com.example.showyourvoice.domain.place.domain;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {

    // 같은 장소·카테고리에 규칙이 여러 개면 먼저 등록된 규칙을 쓴다.
    @EntityGraph(attributePaths = "teacher")
    Optional<AssignmentRule> findFirstByPlaceIdAndCategoryOrderByIdAsc(Long placeId, PostCategory category);
}
