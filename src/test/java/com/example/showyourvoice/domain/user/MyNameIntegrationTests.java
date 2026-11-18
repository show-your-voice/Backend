package com.example.showyourvoice.domain.user;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@Transactional
class MyNameIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;

    @Test
    void returnsOnlyNameOfLoggedInUser() throws Exception {
        String suffix = UUID.randomUUID().toString();
        User student = userRepository.save(User.createStudent("me" + suffix.substring(0, 8),
                "me-" + suffix + "@dsm.hs.kr", "unused", "김학생"));

        mvc.perform(get("/api/users/me").with(user(CustomUserDetails.from(student))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"김학생\"}", JsonCompareMode.STRICT));
    }

    @Test
    void rejectsRequestWithoutLogin() throws Exception {
        mvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
