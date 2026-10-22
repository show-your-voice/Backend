package com.example.showyourvoice.domain.post.domain;

import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    @Override
    @EntityGraph(attributePaths = {"place", "reporter", "assignedTeacher"})
    Optional<Post> findById(Long id);
}
