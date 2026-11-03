package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
                Collections.singletonMap("roomId", null), Map.of("roomId", 0), Map.of("roomId", -1),
                Map.of("improvement", "가".repeat(1001)),
                Collections.singletonMap("posX", null), Map.of("posX", -0.1), Map.of("posX", 1.1),
                Map.of("posX", 0.12345), Collections.singletonMap("posY", null), Map.of("posY", 1.0001));
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
    void rejectsPlaceIdWithoutRoomId() throws Exception {
        Map<String, Object> fields = new HashMap<>();
        fields.put("placeId", place.getId());
        fields.put("roomId", null);

        mvc.perform(createRequest(fields))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));
    }

    @Test
    void rejectsCreationWhenRoomDoesNotExist() throws Exception {
        mvc.perform(createRequest(Map.of("roomId", Long.MAX_VALUE)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("R001"))
                .andExpect(jsonPath("$.errors[0].field").value("roomId"));
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
        mvc.perform(get("/api/posts/{id}", Long.MAX_VALUE).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void returnsBadRequestWhenPostIdIsNotNumeric() throws Exception {
        mvc.perform(get("/api/posts/not-a-number").with(user(CustomUserDetails.from(author))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void listsSelectablePlacesWhenUserIsAuthenticated() throws Exception {
        mvc.perform(get("/api/places").with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].name", place.getId()).value("창의관 4층"))
                .andExpect(jsonPath("$[?(@.id == %s)].imageUrl", place.getId()).value("https://example.com/creative-4f.png"));
    }

    @Test
    void searchesRoomNamesAcrossPlacesAndReturnsTheirPlaceNames() throws Exception {
        String keyword = UUID.randomUUID().toString();
        Room first = roomRepository.save(Room.builder().name(keyword + "-A").place(place).build());
        Room second = roomRepository.save(Room.builder().name(keyword + "-B").place(otherPlace).build());
        entityManager.flush();
        entityManager.clear();

        mvc.perform(get("/api/rooms").param("keyword", keyword)
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(first.getId()))
                .andExpect(jsonPath("$[0].placeId").value(place.getId()))
                .andExpect(jsonPath("$[0].placeName").value("창의관 4층"))
                .andExpect(jsonPath("$[1].id").value(second.getId()))
                .andExpect(jsonPath("$[1].placeName").value("기숙사 2층"));
    }

    @Test
    void returnsEmptyListWhenNoRoomNameContainsKeyword() throws Exception {
        mvc.perform(get("/api/rooms").param("keyword", UUID.randomUUID().toString())
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void returnsRoomsWhenKeywordIsAbsent() throws Exception {
        mvc.perform(get("/api/rooms")
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == %s)].name", room.getId()).value("화장실(남)"))
                .andExpect(jsonPath("$[?(@.id == %s)].name", otherRoom.getId()).value("복도"));
    }

    @Test
    void trimsKeywordAndTreatsSqlWildcardsAsLiteralCharacters() throws Exception {
        String keyword = UUID.randomUUID().toString().substring(0, 8) + "%_";
        Room literal = roomRepository.save(Room.builder().name(keyword + "실").place(place).build());
        roomRepository.save(Room.builder().name(keyword.replace("%_", "AB") + "실").place(place).build());

        mvc.perform(get("/api/rooms").param("keyword", "  " + keyword + "  ")
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(literal.getId()));
    }

    @Test
    void rejectsRoomListWhenSessionIsAbsent() throws Exception {
        mvc.perform(get("/api/rooms"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }
}
