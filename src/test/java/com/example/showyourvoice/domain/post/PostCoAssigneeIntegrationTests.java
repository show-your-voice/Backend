package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PostCoAssigneeIntegrationTests extends PostApiTestSupport {

    private long postId;
    private User coTeacher;

    @BeforeEach
    void createAssignedPostAndCoTeacher() throws Exception {
        assignTeacher(room, PostCategory.FACILITY, teacher);
        postId = responseId(mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.coAssignees").isEmpty()).andReturn());
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", otherUser.getId());
        entityManager.clear();
        coTeacher = userRepository.findById(otherUser.getId()).orElseThrow();
    }

    @Test
    void addsTeacherAndReturnsPersistedDetailWithoutReplacingPrimary() throws Exception {
        mvc.perform(addRequest(coTeacher.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()))
                .andExpect(jsonPath("$.coAssignees.length()").value(1))
                .andExpect(jsonPath("$.coAssignees[0].id").value(coTeacher.getId()))
                .andExpect(jsonPath("$.coAssignees[0].role").value("TEACHER"));

        entityManager.flush();
        entityManager.clear();
        assertThat(coAssigneeCount()).isOne();
        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()))
                .andExpect(jsonPath("$.coAssignees[0].id").value(coTeacher.getId()));
    }

    @Test
    void returnsEmptyCoAssigneesBeforeAnyAddition() throws Exception {
        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.coAssignees").isEmpty());
    }

    @Test
    void rejectsStudentCallerWithoutWritingRelationship() throws Exception {
        mvc.perform(addRequest(coTeacher.getId()).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("P003"));
        assertThat(coAssigneeCount()).isZero();
    }

    @Test
    void rejectsAnonymousCaller() throws Exception {
        mvc.perform(addRequest(coTeacher.getId()))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
        assertThat(coAssigneeCount()).isZero();
    }

    @Test
    void rejectsStudentTarget() throws Exception {
        mvc.perform(addRequest(author.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("P004"));
        assertThat(coAssigneeCount()).isZero();
    }

    @Test
    void rejectsPrimaryAssigneeAsDuplicate() throws Exception {
        mvc.perform(addRequest(teacher.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("P005"));
        assertThat(coAssigneeCount()).isZero();
    }

    @Test
    void rejectsExistingCoAssigneeWithoutDuplicatingRow() throws Exception {
        mvc.perform(addRequest(coTeacher.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk());
        mvc.perform(addRequest(coTeacher.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("P005"));
        assertThat(coAssigneeCount()).isOne();
    }

    @Test
    void rejectsMissingTarget() throws Exception {
        mvc.perform(addRequest(Long.MAX_VALUE).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("U002"));
    }

    @Test
    void rejectsMissingPost() throws Exception {
        mvc.perform(post("/api/posts/{id}/co-assignees", Long.MAX_VALUE)
                        .with(user(CustomUserDetails.from(teacher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("managerId", coTeacher.getId()))))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("P001"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"managerId\":null}", "{\"managerId\":0}", "{\"managerId\":-1}"})
    void rejectsMissingOrNonPositiveManagerId(String body) throws Exception {
        mvc.perform(post("/api/posts/{id}/co-assignees", postId)
                        .with(user(CustomUserDetails.from(teacher)))
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
        assertThat(coAssigneeCount()).isZero();
    }

    @Test
    void deletesRelationshipsAlongsidePostAndPreservesTeacher() throws Exception {
        mvc.perform(addRequest(coTeacher.getId()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk());
        mvc.perform(delete("/api/posts/{id}", postId).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();
        assertThat(coAssigneeCount()).isZero();
        assertThat(postRepository.existsById(postId)).isFalse();
        assertThat(userRepository.existsById(coTeacher.getId())).isTrue();
    }

    private MockHttpServletRequestBuilder addRequest(long managerId) {
        return post("/api/posts/{id}/co-assignees", postId).contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("managerId", managerId)));
    }

    private int coAssigneeCount() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM tbl_post_co_assignee WHERE post_id = ?", Integer.class, postId);
    }
}
