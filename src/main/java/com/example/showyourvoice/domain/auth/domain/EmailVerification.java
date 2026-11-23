package com.example.showyourvoice.domain.auth.domain;

import com.example.showyourvoice.domain.auth.exception.VerificationAttemptsExceededException;
import com.example.showyourvoice.domain.auth.exception.VerificationCodeExpiredException;
import com.example.showyourvoice.domain.auth.exception.VerificationCodeMismatchException;
import com.example.showyourvoice.global.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "tbl_email_verification")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification extends BaseTimeEntity {

    public static final int MAX_ATTEMPTS = 5;
    public static final Duration CODE_TTL = Duration.ofMinutes(3);
    // 인증을 마친 뒤 이 시간 안에 가입해야 한다. 오래전에 인증한 이메일로 가입하는 것을 막는다.
    public static final Duration SIGN_UP_WINDOW = Duration.ofMinutes(30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false, length = 6)
    private String code;

    @Column(nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private int remainingAttempts;

    private LocalDateTime verifiedAt;

    public static EmailVerification issue(String email, String code, LocalDateTime now) {
        EmailVerification verification = new EmailVerification();
        verification.email = email;
        verification.reissue(code, now);
        return verification;
    }

    // 재전송하면 새 번호로 바꾸고 유효시간·남은 기회·인증 여부를 처음부터 다시 시작한다.
    public void reissue(String code, LocalDateTime now) {
        this.code = code;
        this.expiresAt = now.plus(CODE_TTL);
        this.remainingAttempts = MAX_ATTEMPTS;
        this.verifiedAt = null;
    }

    public void verify(String input, LocalDateTime now) {
        if (remainingAttempts <= 0) {
            throw new VerificationAttemptsExceededException();
        }
        if (!now.isBefore(expiresAt)) {
            throw new VerificationCodeExpiredException();
        }
        if (!code.equals(input)) {
            remainingAttempts--;
            throw new VerificationCodeMismatchException(remainingAttempts);
        }
        if (verifiedAt == null) {
            verifiedAt = now;
        }
    }

    public boolean allowsSignUp(LocalDateTime now) {
        return verifiedAt != null && now.isBefore(verifiedAt.plus(SIGN_UP_WINDOW));
    }
}
