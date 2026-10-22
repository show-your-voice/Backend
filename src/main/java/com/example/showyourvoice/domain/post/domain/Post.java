package com.example.showyourvoice.domain.post.domain;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tbl_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 1000)
    private String improvement;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private User assignedTeacher;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    private LocalDateTime processingStartedAt;

    private LocalDateTime resolvedAt;

    @Builder
    private Post(String title, String content, String improvement, PostCategory category,
                 Place place, User reporter, User assignedTeacher) {
        this.title = title.strip();
        this.content = content.strip();
        this.improvement = improvement == null || improvement.isBlank() ? null : improvement.strip();
        this.category = category;
        this.place = place;
        this.reporter = reporter;
        this.assignedTeacher = assignedTeacher;
        this.status = PostStatus.RECEIVED;
    }
}
