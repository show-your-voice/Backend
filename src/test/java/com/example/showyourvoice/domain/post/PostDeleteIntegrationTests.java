package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PostDeleteIntegrationTests extends PostApiTestSupport {

    private long postId;

    @BeforeEach
    void createPostWithImage() throws Exception {
        postId = responseId(mvc.perform(createRequest(Map.of())).andReturn());
        jdbc.update("INSERT INTO tbl_post_image (post_id, image_url, sort_order) VALUES (?, 'https://example.com/a.jpg', 0)",
                postId);
    }

    @Test
    void deletesOwnPostWithImagesWhenAuthorRequests() throws Exception {
        mvc.perform(delete("/api/posts/{id}", postId).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isNoContent());

        entityManager.flush();
        assertThat(countRows("tbl_post", "id")).isZero();
        assertThat(countRows("tbl_post_image", "post_id")).isZero();
    }

    @Test
    void deletesAnyPostWhenTeacherRequests() throws Exception {
        mvc.perform(delete("/api/posts/{id}", postId).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isNoContent());

        entityManager.flush();
        assertThat(countRows("tbl_post", "id")).isZero();
    }

    @Test
    void rejectsDeletionWhenOtherStudentRequests() throws Exception {
        mvc.perform(delete("/api/posts/{id}", postId).with(user(CustomUserDetails.from(otherUser))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("P002"));

        entityManager.flush();
        assertThat(countRows("tbl_post", "id")).isOne();
        assertThat(countRows("tbl_post_image", "post_id")).isOne();
    }

    @Test
    void returnsNotFoundWhenDeletingMissingPost() throws Exception {
        mvc.perform(delete("/api/posts/{id}", Long.MAX_VALUE).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("P001"));
    }

    @Test
    void rejectsDeletionWhenSessionIsAbsent() throws Exception {
        mvc.perform(delete("/api/posts/{id}", postId))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    private int countRows(String table, String postIdColumn) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table + " WHERE " + postIdColumn + " = ?",
                Integer.class, postId);
    }
}
