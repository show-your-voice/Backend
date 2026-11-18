package com.example.showyourvoice.domain.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.json.JsonCompareMode;

class PostAssignedIntegrationTests extends PostApiTestSupport {

    @Test
    void listsPrimaryAndCoAssignedPostsOnceAndFiltersByStatus() throws Exception {
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", otherUser.getId());
        entityManager.clear();
        User otherTeacher = userRepository.findById(otherUser.getId()).orElseThrow();
        assignTeacher(room, PostCategory.FACILITY, teacher);
        assignTeacher(otherRoom, PostCategory.FACILITY, otherTeacher);

        long primary = createPost(Map.of("title", "기본 담당 신고"));
        long coAssigned = createPost(Map.of("title", "공동 담당 신고", "roomId", samePlaceRoom.getId()));
        long anotherTeachers = createPost(Map.of("title", "다른 담당자의 신고", "roomId", otherRoom.getId()));
        createPost(Map.of("title", "담당자가 없는 신고", "roomId", samePlaceRoom.getId()));
        mvc.perform(post("/api/posts/{id}/co-assignees", coAssigned)
                        .with(user(CustomUserDetails.from(otherTeacher)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonMapper.writeValueAsString(Map.of("managerId", teacher.getId()))))
                .andExpect(status().isOk());
        jdbc.update("UPDATE tbl_post SET status = 'IN_PROGRESS' WHERE id = ?", coAssigned);
        entityManager.clear();

        mvc.perform(get("/api/posts/assigned").with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(coAssigned))
                .andExpect(jsonPath("$[0].status.code").value("IN_PROGRESS"))
                .andExpect(jsonPath("$[1].id").value(primary))
                .andExpect(jsonPath("$[1].roomName").value(room.getName()));

        mvc.perform(get("/api/posts/assigned").param("status", "IN_PROGRESS")
                        .with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(coAssigned));

        mvc.perform(get("/api/posts/assigned").with(user(CustomUserDetails.from(otherTeacher))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(anotherTeachers));
    }

    @Test
    void distinguishesEmptyResultsFromInvalidStatusAndMissingPermissions() throws Exception {
        mvc.perform(get("/api/posts/assigned").with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mvc.perform(get("/api/posts/assigned").param("status", "UNKNOWN")
                        .with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("C001"));
        mvc.perform(get("/api/posts/assigned").with(user(CustomUserDetails.from(author))))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A002"));
        mvc.perform(get("/api/posts/assigned"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void returnsOnlyNameAndCurrentSessionRoleToLoggedInUsers() throws Exception {
        mvc.perform(get("/api/users/me/session").with(user(CustomUserDetails.from(teacher))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"이선생\",\"role\":\"TEACHER\"}", JsonCompareMode.STRICT));
        mvc.perform(get("/api/users/me/session").with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"name\":\"김학생\",\"role\":\"USER\"}", JsonCompareMode.STRICT));
        mvc.perform(get("/api/users/me/session"))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    private long createPost(Map<String, Object> body) throws Exception {
        return responseId(mvc.perform(createRequest(body)).andExpect(status().isCreated()).andReturn());
    }
}
