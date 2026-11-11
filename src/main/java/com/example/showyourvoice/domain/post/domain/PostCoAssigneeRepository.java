package com.example.showyourvoice.domain.post.domain;

import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostCoAssigneeRepository extends JpaRepository<PostCoAssignee, Long> {

    @EntityGraph(attributePaths = "manager")
    List<PostCoAssignee> findAllByPostIdOrderByIdAsc(Long postId);

    boolean existsByPostIdAndManagerId(Long postId, Long managerId);

    void deleteAllByPostId(Long postId);
}
