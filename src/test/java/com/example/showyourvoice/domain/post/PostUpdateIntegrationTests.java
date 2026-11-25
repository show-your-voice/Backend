package com.example.showyourvoice.domain.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.JsonNode;

class PostUpdateIntegrationTests extends PostApiTestSupport {

    @Test
    void savesEditableFieldsAndPreservesAuthorAndStatusHistory() throws Exception {
        long id = responseId(mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.canEdit").value(true)).andReturn());
        mvc.perform(post("/api/posts/{id}/co-assignees", id).with(user(CustomUserDetails.from(teacher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("managerId", teacher.getId()))))
                .andExpect(status().isOk());
        JsonNode before = jsonMapper.readTree(mvc.perform(patch("/api/posts/{id}/status", id)
                        .with(user(CustomUserDetails.from(teacher))).contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(false))
                .andReturn().getResponse().getContentAsString());

        assignTeacher(otherRoom, PostCategory.NETWORK, teacher);
        mvc.perform(updateRequest(id, Map.of("title", "수정한 제목", "content", "수정한 본문",
                        "improvement", "개선 제안", "category", "NETWORK", "roomId", otherRoom.getId(),
                        "posX", 0.125, "posY", 0.875)).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.canEdit").value(true));

        entityManager.flush();
        entityManager.clear();
        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.author.id").value(author.getId()))
                .andExpect(jsonPath("$.title").value("수정한 제목")).andExpect(jsonPath("$.content").value("수정한 본문"))
                .andExpect(jsonPath("$.improvement").value("개선 제안")).andExpect(jsonPath("$.category.code").value("NETWORK"))
                .andExpect(jsonPath("$.place.id").value(otherPlace.getId())).andExpect(jsonPath("$.room.id").value(otherRoom.getId()))
                .andExpect(jsonPath("$.posX").value(0.125)).andExpect(jsonPath("$.posY").value(0.875))
                .andExpect(jsonPath("$.status.code").value("IN_PROGRESS"))
                .andExpect(jsonPath("$.receivedAt").value(before.path("receivedAt").asString()))
                .andExpect(jsonPath("$.inProgressAt").value(before.path("inProgressAt").asString()))
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId())).andExpect(jsonPath("$.coAssignees").isEmpty());
    }

    @Test
    void rejectsNonAuthorsIncludingAssignedTeachersAndAnonymousRequests() throws Exception {
        assignTeacher(room, PostCategory.FACILITY, teacher);
        long id = responseId(mvc.perform(createRequest(Map.of("title", "원래 제목"))).andExpect(status().isCreated()).andReturn());
        for (User viewer : List.of(otherUser, teacher)) {
            mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(viewer))))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.canEdit").value(false));
            mvc.perform(updateRequest(id, Map.of()).with(user(CustomUserDetails.from(viewer))))
                    .andExpect(status().isForbidden());
        }
        mvc.perform(updateRequest(id, Map.of())).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.title").value("원래 제목"));
    }

    @Test
    void rejectsInvalidFieldsAndMissingResources() throws Exception {
        long id = responseId(mvc.perform(createRequest(Map.of())).andExpect(status().isCreated()).andReturn());
        mvc.perform(updateRequest(id, Map.of("title", " ")).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("title"));
        mvc.perform(updateRequest(id, Map.of("roomId", Long.MAX_VALUE)).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isNotFound());
        mvc.perform(updateRequest(Long.MAX_VALUE, Map.of()).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isNotFound());
    }
}
