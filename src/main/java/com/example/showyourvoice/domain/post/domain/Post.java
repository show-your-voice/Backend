package com.example.showyourvoice.domain.post.domain;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.user.domain.Role;
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
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "tbl_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(columnDefinition = "TEXT")
    private String improvement;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PostCategory category;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private PostStatus status;

    @Column(name = "pos_x", nullable = false, precision = 5, scale = 4)
    private BigDecimal posX;

    @Column(name = "pos_y", nullable = false, precision = 5, scale = 4)
    private BigDecimal posY;

    private LocalDateTime inProgressAt;

    private LocalDateTime resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Builder
    private Post(String title, String content, String improvement, PostCategory category,
                 Place place, BigDecimal posX, BigDecimal posY, User author, User assignee) {
        this.title = title.strip();
        this.content = content.strip();
        this.improvement = improvement == null || improvement.isBlank() ? null : improvement.strip();
        this.category = category;
        this.place = place;
        this.posX = posX;
        this.posY = posY;
        this.author = author;
        this.assignee = assignee;
        this.status = PostStatus.UNRESOLVED;
    }

    public boolean isDeletableBy(Long userId, Role role) {
        return role == Role.TEACHER || author.getId().equals(userId);
    }
}
