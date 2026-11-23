package com.example.showyourvoice.domain.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import tools.jackson.databind.json.JsonMapper;

// 틀린 횟수가 커밋돼 남는지 확인해야 하므로 테스트 트랜잭션을 쓰지 않고, 만든 데이터를 끝에서 직접 지운다.
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "notification.mail.from=noreply@show-your-voice.test"
})
@AutoConfigureMockMvc
class EmailVerificationIntegrationTests {

    private static final Pattern CODE = Pattern.compile("인증번호: (\\d{6})");

    @MockitoBean private JavaMailSender mailSender;
    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbc;

    private String suffix;
    private String email;
    private String loginId;

    @BeforeEach
    void prepare() {
        suffix = UUID.randomUUID().toString().substring(0, 8);
        email = "verify-" + suffix + "@dsm.hs.kr";
        loginId = "verify" + suffix;
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("DELETE FROM tbl_email_verification WHERE email LIKE ?", "%" + suffix + "%");
        jdbc.update("DELETE FROM tbl_user WHERE email LIKE ?", "%" + suffix + "%");
    }

    @Test
    void sendsSixDigitCodeWithThreeMinutesAndFiveAttempts() throws Exception {
        sendCode(email)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.expiresInSeconds").value(180))
                .andExpect(jsonPath("$.remainingAttempts").value(5));

        SimpleMailMessage message = lastMail();
        assertThat(message.getTo()).containsExactly(email);
        assertThat(message.getFrom()).isEqualTo("noreply@show-your-voice.test");
        assertThat(message.getSubject()).matches("\\[너목보] 회원가입 인증번호: \\d{6}");
        assertThat(message.getText()).containsPattern("인증번호: \\d{6}").contains("3분 안에").contains("5번");
    }

    @Test
    void verifiedEmailCanSignUpOnlyOnce() throws Exception {
        sendCode(email).andExpect(status().isOk());

        verifyCode(email, codeInLastMail()).andExpect(status().isNoContent());
        signUp(loginId, email).andExpect(status().isCreated());

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tbl_email_verification WHERE email = ?", Integer.class, email))
                .isZero();
    }

    @Test
    void rejectsSignUpWithoutVerification() throws Exception {
        signUp(loginId, email)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E005"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        sendCode(email).andExpect(status().isOk());
        signUp(loginId, email).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E005"));
        assertThat(userRepository.existsByEmail(email)).isFalse();
    }

    @Test
    void countsDownAttemptsAndRequiresResendAfterFiveMisses() throws Exception {
        sendCode(email).andExpect(status().isOk());
        String code = codeInLastMail();

        for (int remaining = 4; remaining >= 0; remaining--) {
            verifyCode(email, wrongCode(code))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("E002"))
                    .andExpect(jsonPath("$.errors[0].field").value("code"))
                    .andExpect(jsonPath("$.remainingAttempts").value(remaining));
        }

        verifyCode(email, code)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("E004"));

        sendCode(email).andExpect(status().isOk()).andExpect(jsonPath("$.remainingAttempts").value(5));
        verifyCode(email, codeInLastMail()).andExpect(status().isNoContent());
    }

    @Test
    void resendResetsAttemptsAndReplacesCode() throws Exception {
        sendCode(email).andExpect(status().isOk());
        String first = codeInLastMail();
        verifyCode(email, wrongCode(first)).andExpect(jsonPath("$.remainingAttempts").value(4));

        sendCode(email).andExpect(status().isOk());
        String second = codeInLastMail();

        verifyCode(email, wrongCode(second))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.remainingAttempts").value(4));
        verifyCode(email, second).andExpect(status().isNoContent());
    }

    @Test
    void rejectsExpiredCode() throws Exception {
        sendCode(email).andExpect(status().isOk());
        String code = codeInLastMail();
        jdbc.update("UPDATE tbl_email_verification SET expires_at = CURRENT_TIMESTAMP(6) - INTERVAL 1 SECOND WHERE email = ?", email);

        verifyCode(email, code)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E003"));
    }

    @Test
    void rejectsSignUpWhenVerificationIsOlderThanThirtyMinutes() throws Exception {
        sendCode(email).andExpect(status().isOk());
        verifyCode(email, codeInLastMail()).andExpect(status().isNoContent());
        jdbc.update("UPDATE tbl_email_verification SET verified_at = CURRENT_TIMESTAMP(6) - INTERVAL 31 MINUTE WHERE email = ?", email);

        signUp(loginId, email).andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("E005"));
    }

    @Test
    void rejectsVerificationThatWasNeverRequested() throws Exception {
        verifyCode(email, "123456")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E001"));
    }

    @Test
    void rejectsRegisteredOrNonSchoolEmailWithoutSendingMail() throws Exception {
        userRepository.save(User.createStudent(loginId, email, "unused", "김학생"));

        sendCode(email).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("U001"));
        sendCode("verify-" + suffix + "@gmail.com")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("email"));

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void keepsNoCodeWhenMailDeliveryFails() throws Exception {
        doThrow(new MailSendException("SMTP 연결 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        sendCode(email)
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("E006"));

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM tbl_email_verification WHERE email = ?", Integer.class, email))
                .isZero();
    }

    private ResultActions sendCode(String address) throws Exception {
        return mvc.perform(post("/api/auth/email-verifications").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", address))));
    }

    private ResultActions verifyCode(String address, String code) throws Exception {
        return mvc.perform(post("/api/auth/email-verifications/verify").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("email", address, "code", code))));
    }

    private ResultActions signUp(String id, String address) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of(
                        "loginId", id, "email", address, "password", "LocalTest123!", "name", "새학생"))));
    }

    private SimpleMailMessage lastMail() {
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, times(1)).send(captor.capture());
        reset(mailSender);
        return captor.getValue();
    }

    private String codeInLastMail() {
        Matcher matcher = CODE.matcher(lastMail().getText());
        assertThat(matcher.find()).isTrue();
        return matcher.group(1);
    }

    private String wrongCode(String code) {
        return "%06d".formatted((Integer.parseInt(code) + 1) % 1_000_000);
    }
}
