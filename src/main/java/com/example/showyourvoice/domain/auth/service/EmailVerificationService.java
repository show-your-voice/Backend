package com.example.showyourvoice.domain.auth.service;

import com.example.showyourvoice.domain.auth.controller.dto.EmailVerificationSendResponse;
import com.example.showyourvoice.domain.auth.domain.EmailVerification;
import com.example.showyourvoice.domain.auth.domain.EmailVerificationRepository;
import com.example.showyourvoice.domain.auth.exception.EmailNotVerifiedException;
import com.example.showyourvoice.domain.auth.exception.EmailVerificationNotFoundException;
import com.example.showyourvoice.domain.auth.exception.VerificationCodeMismatchException;
import com.example.showyourvoice.domain.auth.exception.VerificationMailFailedException;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.DuplicateEmailException;
import com.example.showyourvoice.global.config.MailNotificationProperties;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailVerificationService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final EmailVerificationRepository emailVerificationRepository;
    private final UserRepository userRepository;
    private final JavaMailSender mailSender;
    private final MailNotificationProperties mailProperties;

    // 메일을 보내지 못하면 트랜잭션을 되돌려, 사용자가 받지 못한 번호가 저장되지 않게 한다.
    @Transactional
    public EmailVerificationSendResponse sendCode(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new DuplicateEmailException();
        }

        String code = "%06d".formatted(RANDOM.nextInt(1_000_000));
        LocalDateTime now = now();
        EmailVerification verification = emailVerificationRepository.findByEmailForUpdate(email).orElse(null);
        if (verification == null) {
            verification = emailVerificationRepository.save(EmailVerification.issue(email, code, now));
        } else {
            verification.reissue(code, now);
        }

        sendMail(email, code);
        return EmailVerificationSendResponse.from(verification);
    }

    // 번호가 틀려도 줄어든 남은 기회는 저장돼야 하므로 불일치 예외로는 롤백하지 않는다.
    @Transactional(noRollbackFor = VerificationCodeMismatchException.class)
    public void verify(String email, String code) {
        emailVerificationRepository.findByEmailForUpdate(email)
                .orElseThrow(EmailVerificationNotFoundException::new)
                .verify(code, now());
    }

    // 가입에 한 번 쓴 인증은 지운다. 가입이 실패하면 같은 트랜잭션이 롤백돼 인증도 되살아난다.
    @Transactional
    public void consumeForSignUp(String email) {
        LocalDateTime now = now();
        EmailVerification verification = emailVerificationRepository.findByEmailForUpdate(email)
                .filter(found -> found.allowsSignUp(now))
                .orElseThrow(EmailNotVerifiedException::new);
        emailVerificationRepository.delete(verification);
    }

    private void sendMail(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailProperties.from());
        message.setTo(email);
        message.setSubject("[너목보] 회원가입 인증번호: " + code);
        message.setText("""
                너목보 회원가입 인증번호입니다.

                인증번호: %s

                %d분 안에 입력해 주세요. 입력 기회는 %d번입니다.
                본인이 요청하지 않았다면 이 메일을 무시하세요.
                """.formatted(code, EmailVerification.CODE_TTL.toMinutes(), EmailVerification.MAX_ATTEMPTS));
        try {
            mailSender.send(message);
        } catch (MailException e) {
            log.warn("이메일 인증번호 발송 실패: {}", email, e);
            throw new VerificationMailFailedException();
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.MICROS);
    }
}
