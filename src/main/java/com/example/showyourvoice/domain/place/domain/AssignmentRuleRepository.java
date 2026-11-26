package com.example.showyourvoice.domain.place.domain;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {

    // 같은 실·카테고리에 규칙이 여러 개면 먼저 등록된 규칙을 쓴다.
    @EntityGraph(attributePaths = "teacher")
    Optional<AssignmentRule> findFirstByRoomIdAndCategoryOrderByIdAsc(Long roomId, PostCategory category);

    @EntityGraph(attributePaths = {"teacher", "room.place"})
    @Query("""
            select r from AssignmentRule r
            where r.id in (select min(a.id) from AssignmentRule a group by a.room.id, a.category)
            order by r.room.place.name, r.room.name, r.room.id, r.category
            """)
    List<AssignmentRule> findEffectiveRules();

    @Modifying
    @Query("delete from AssignmentRule r where r.room.id = :roomId and r.category = :category")
    int deleteRules(@Param("roomId") Long roomId, @Param("category") PostCategory category);
}
