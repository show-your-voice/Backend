package com.example.showyourvoice.domain.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@Transactional
class AuthLoginIdIntegrationTests {

    private static final String PASSWORD = "LocalTest123!";

    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private User student;

    @BeforeEach
    void createStudent() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        student = userRepository.save(User.createStudent("student" + suffix, "student-" + suffix + "@dsm.hs.kr",
                passwordEncoder.encode(PASSWORD), "김학생"));
    }

    @Test
    void logsInWithLoginId() throws Exception {
        mvc.perform(json(post("/api/auth/login"), Map.of("loginId", student.getLoginId(), "password", PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.loginId").value(student.getLoginId()))
                .andExpect(jsonPath("$.email").value(student.getEmail()));
    }

    @Test
    void rejectsLoginWithEmailInsteadOfLoginId() throws Exception {
        mvc.perform(json(post("/api/auth/login"), Map.of("loginId", student.getEmail(), "password", PASSWORD)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A003"));
    }

    @Test
    void signsUpWithLoginId() throws Exception {
        String loginId = "new" + UUID.randomUUID().toString().substring(0, 8);
        mvc.perform(json(post("/api/auth/signup"), signUp(loginId, loginId + "@dsm.hs.kr")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.loginId").value(loginId))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void rejectsSignUpWhenLoginIdIsTaken() throws Exception {
        String email = "other-" + UUID.randomUUID().toString().substring(0, 8) + "@dsm.hs.kr";
        mvc.perform(json(post("/api/auth/signup"), signUp(student.getLoginId(), email)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("U003"));
    }

    @Test
    void rejectsSignUpWhenLoginIdFormatIsInvalid() throws Exception {
        String email = "upper-" + UUID.randomUUID().toString().substring(0, 8) + "@dsm.hs.kr";
        mvc.perform(json(post("/api/auth/signup"), signUp("Student_01", email)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("loginId"));
    }

    private Map<String, Object> signUp(String loginId, String email) {
        Map<String, Object> body = new HashMap<>();
        body.put("loginId", loginId);
        body.put("email", email);
        body.put("password", PASSWORD);
        body.put("name", "새학생");
        return body;
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Map<String, Object> body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
    }
}
