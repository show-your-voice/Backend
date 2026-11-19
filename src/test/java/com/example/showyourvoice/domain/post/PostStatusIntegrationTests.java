package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class PostStatusIntegrationTests extends PostApiTestSupport {

    @Test
    void teacherCanChangeTheirOwnUnassignedPostAndTimestampsPersist() throws Exception {
        long postId = responseId(mvc.perform(createRequest(Map.of()).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.canChangeStatus").value(true))
                .andReturn());

        MvcResult started = mvc.perform(change(postId, "IN_PROGRESS", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.resolvedAt").isEmpty())
                .andReturn();
        String startedAt = jsonMapper.readTree(started.getResponse().getContentAsString()).path("inProgressAt").asString();
        assertThat(startedAt).isNotBlank();

        MvcResult completed = mvc.perform(change(postId, "RESOLVED", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("RESOLVED"))
                .andExpect(jsonPath("$.inProgressAt").value(startedAt))
                .andReturn();
        String resolvedAt = jsonMapper.readTree(completed.getResponse().getContentAsString()).path("resolvedAt").asString();
        assertThat(resolvedAt).isNotBlank();

        mvc.perform(change(postId, "RESOLVED", teacher))
                .andExpect(status().isOk()).andExpect(jsonPath("$.resolvedAt").value(resolvedAt));

        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canChangeStatus").value(true))
                .andExpect(jsonPath("$.status.code").value("RESOLVED"))
                .andExpect(jsonPath("$.inProgressAt").value(startedAt))
                .andExpect(jsonPath("$.resolvedAt").value(resolvedAt));

        mvc.perform(change(postId, "UNRESOLVED", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status.code").value("UNRESOLVED"))
                .andExpect(jsonPath("$.receivedAt").isNotEmpty())
                .andExpect(jsonPath("$.inProgressAt").isEmpty())
                .andExpect(jsonPath("$.resolvedAt").isEmpty());
    }

    @Test
    void primaryAndCoAssignedTeachersCanChangeStudentReports() throws Exception {
        assignTeacher(room, PostCategory.FACILITY, teacher);
        long primary = createPost(Map.of());
        long coAssigned = createPost(Map.of("roomId", otherRoom.getId()));

        mvc.perform(post("/api/posts/{id}/co-assignees", coAssigned)
                        .with(user(CustomUserDetails.from(teacher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("managerId", teacher.getId()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canChangeStatus").value(true));

        mvc.perform(change(primary, "RESOLVED", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inProgressAt").isEmpty())
                .andExpect(jsonPath("$.resolvedAt").isNotEmpty());
        mvc.perform(change(coAssigned, "IN_PROGRESS", teacher))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inProgressAt").isNotEmpty());
    }

    @Test
    void rejectsUnrelatedTeacherStudentAuthorAndInvalidRequests() throws Exception {
        long postId = createPost(Map.of());
        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canChangeStatus").value(false));
        mvc.perform(change(postId, "RESOLVED", teacher))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("P006"));
        mvc.perform(change(postId, "RESOLVED", author))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("P006"));
        mvc.perform(change(postId, "UNKNOWN", teacher))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
        mvc.perform(patch("/api/posts/{id}/status", postId))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canChangeStatus").value(false))
                .andExpect(jsonPath("$.status.code").value("UNRESOLVED"));
    }

    private long createPost(Map<String, Object> body) throws Exception {
        return responseId(mvc.perform(createRequest(body)).andExpect(status().isCreated()).andReturn());
    }

    private MockHttpServletRequestBuilder change(long postId, String newStatus, User actor) {
        return patch("/api/posts/{id}/status", postId)
                .with(user(CustomUserDetails.from(actor)))
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(Map.of("status", newStatus)));
    }
}
