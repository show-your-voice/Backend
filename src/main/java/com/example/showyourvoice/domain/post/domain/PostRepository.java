package com.example.showyourvoice.domain.post.domain;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PostRepository extends JpaRepository<Post, Long> {

    // 필터 값이 null이면 해당 조건은 적용하지 않는다.
    @Query("""
            select p from Post p
            join fetch p.author
            join fetch p.place
            where (:placeId is null or p.place.id = :placeId)
              and (:status is null or p.status = :status)
            order by p.createdAt desc
            """)
    List<Post> findAllByFilter(@Param("placeId") Long placeId, @Param("status") PostStatus status);

    @Override
    @EntityGraph(attributePaths = {"place", "author", "assignee"})
    Optional<Post> findById(Long id);
}
