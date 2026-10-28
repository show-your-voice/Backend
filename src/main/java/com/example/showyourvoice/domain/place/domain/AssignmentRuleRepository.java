package com.example.showyourvoice.domain.place.domain;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.Role;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRuleRepository extends JpaRepository<AssignmentRule, Long> {

    // 같은 장소·카테고리에 규칙이 여러 개면 먼저 등록된 규칙을 쓴다.
    @EntityGraph(attributePaths = "teacher")
    Optional<AssignmentRule> findFirstByPlaceIdAndCategoryOrderByIdAsc(Long placeId, PostCategory category);

    @Query("""
            select r from AssignmentRule r
            join fetch r.teacher
            join fetch r.place
            where r.teacher.role = :role
              and not exists (
                  select earlier.id from AssignmentRule earlier
                  where earlier.place = r.place and earlier.category = r.category and earlier.id < r.id
              )
            order by r.place.name asc, r.place.id asc, r.id asc
            """)
    List<AssignmentRule> findAllEffectiveByTeacherRole(@Param("role") Role role);
}
