package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

class PostValidationIntegrationTests extends PostApiTestSupport {

    static Stream<Map<String, Object>> invalidFields() {
        return Stream.of(
                Map.of("title", "   "), Map.of("title", "가".repeat(101)),
                Map.of("content", ""), Map.of("content", "가".repeat(5001)),
                Map.of("category", "UNKNOWN"), Collections.singletonMap("category", null),
                Collections.singletonMap("placeId", null), Map.of("placeId", 0), Map.of("placeId", -1),
                Map.of("improvement", "가".repeat(1001)));
    }

    @ParameterizedTest
    @MethodSource("invalidFields")
    void rejectsInvalidInputWithoutPersistingPostWhenRequestViolatesContract(Map<String, Object> fields)
            throws Exception {
        long count = postRepository.count();

        mvc.perform(createRequest(fields))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));

        assertThat(postRepository.count()).isEqualTo(count);
    }

    @Test
    void reportsInvalidFieldWhenTitleIsMissing() throws Exception {
        mvc.perform(createRequest(Collections.singletonMap("title", null)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("title"));
    }

    @Test
    void rejectsCreationWhenPlaceDoesNotExist() throws Exception {
        mvc.perform(createRequest(Map.of("placeId", Long.MAX_VALUE)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("L001"))
                .andExpect(jsonPath("$.errors[0].field").value("placeId"));
    }

    @Test
    void reportsCategoryFieldWhenCategoryIsUnknown() throws Exception {
        mvc.perform(createRequest(Map.of("category", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("category"));
    }

    @Test
    void rejectsCreationWhenSessionIsAbsent() throws Exception {
        long count = postRepository.count();

        mvc.perform(post("/api/posts").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));

        assertThat(postRepository.count()).isEqualTo(count);
    }

    @Test
    void returnsNotFoundWhenPostDoesNotExist() throws Exception {
        mvc.perform(get("/api/posts/{id}", Long.MAX_VALUE).with(user(CustomUserDetails.from(reporter))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void returnsBadRequestWhenPostIdIsNotNumeric() throws Exception {
        mvc.perform(get("/api/posts/not-a-number").with(user(CustomUserDetails.from(reporter))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void listsSelectablePlacesWhenUserIsAuthenticated() throws Exception {
        mvc.perform(get("/api/places").with(user(CustomUserDetails.from(reporter))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].name", place.getId()).value("화장실(남)"));
    }
}
