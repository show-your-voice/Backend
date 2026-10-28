package com.example.showyourvoice.domain.user.domain;

import com.example.showyourvoice.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Entity
@Table(name = "tbl_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String loginId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, length = 50)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, length = 20)
    private Role role;

    @Builder(access = AccessLevel.PRIVATE)
    private User(String loginId, String email, String password, String name, Role role) {
        this.loginId = loginId;
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role;
    }

    public static User createStudent(String loginId, String email, String encodedPassword, String name) {
        return User.builder()
                .loginId(loginId)
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(Role.USER)
                .build();
    }
}
