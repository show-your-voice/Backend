package com.example.showyourvoice.domain.post.domain;

import com.example.showyourvoice.domain.post.exception.InvalidPostCoAssigneeException;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tbl_post_co_assignee", uniqueConstraints =
        @UniqueConstraint(name = "uk_post_co_assignee", columnNames = {"post_id", "manager_id"}))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCoAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "manager_id", nullable = false)
    private User manager;

    public static PostCoAssignee assign(Post post, User manager) {
        if (manager.getRole() != Role.TEACHER) {
            throw new InvalidPostCoAssigneeException();
        }
        PostCoAssignee assignment = new PostCoAssignee();
        assignment.post = post;
        assignment.manager = manager;
        return assignment;
    }
}
