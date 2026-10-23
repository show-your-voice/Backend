package com.example.showyourvoice.domain.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Map;
import org.junit.jupiter.api.Test;

class PostListIntegrationTests extends PostApiTestSupport {

    @Test
    void listsPostsOfPlaceNewestFirst() throws Exception {
        long first = responseId(mvc.perform(createRequest(Map.of("title", "먼저 쓴 글"))).andReturn());
        long second = responseId(mvc.perform(createRequest(Map.of("title", "나중에 쓴 글"))).andReturn());

        mvc.perform(get("/api/posts").param("placeId", place.getId().toString())
                        .with(user(CustomUserDetails.from(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(second))
                .andExpect(jsonPath("$[1].id").value(first))
                .andExpect(jsonPath("$[0].title").value("나중에 쓴 글"))
                .andExpect(jsonPath("$[0].content").value("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다."))
                .andExpect(jsonPath("$[0].status.code").value("UNRESOLVED"))
                .andExpect(jsonPath("$[0].status.label").value("미해결"))
                .andExpect(jsonPath("$[0].authorName").value("김학생"))
                .andExpect(jsonPath("$[0].placeName").value("창의관 4층"))
                .andExpect(jsonPath("$[0].receivedAt").isNotEmpty());
    }

    @Test
    void filtersByPlaceAndStatusTogether() throws Exception {
        long inProgress = responseId(mvc.perform(createRequest(Map.of())).andReturn());
        mvc.perform(createRequest(Map.of()));
        mvc.perform(createRequest(Map.of("placeId", otherPlace.getId())));
        jdbc.update("UPDATE tbl_post SET status = 'IN_PROGRESS' WHERE id = ?", inProgress);
        entityManager.clear();

        mvc.perform(get("/api/posts").param("placeId", place.getId().toString()).param("status", "IN_PROGRESS")
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(inProgress));

        mvc.perform(get("/api/posts").param("placeId", otherPlace.getId().toString())
                        .with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].placeName").value("기숙사 2층"));
    }

    @Test
    void rejectsUnknownStatusFilter() throws Exception {
        mvc.perform(get("/api/posts").param("status", "UNKNOWN").with(user(CustomUserDetails.from(author))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
    }

    @Test
    void rejectsListWhenSessionIsAbsent() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }
}
