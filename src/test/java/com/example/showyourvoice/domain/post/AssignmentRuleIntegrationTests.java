package com.example.showyourvoice.domain.post;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

class AssignmentRuleIntegrationTests extends PostApiTestSupport {

    private static final String RULES = "/api/assignment-rules";
    private static final String RULE = RULES + "/{roomId}/{category}";

    @Test
    void changesToRulesApplyOnlyToNewMatchingReports() throws Exception {
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", otherUser.getId());
        entityManager.clear();

        asTeacher(create(teacher.getId()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomId").value(room.getId()))
                .andExpect(jsonPath("$.roomName").value(room.getName()))
                .andExpect(jsonPath("$.placeName").value(place.getName()))
                .andExpect(jsonPath("$.category").value("FACILITY"))
                .andExpect(jsonPath("$.teacherId").value(teacher.getId()))
                .andExpect(jsonPath("$.teacherName").value(teacher.getName()));
        long original = responseId(mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId())).andReturn());

        asTeacher(create(otherUser.getId())).andExpect(status().isConflict());
        asTeacher(update(otherUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.teacherId").value(otherUser.getId()));
        long reassigned = responseId(mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee.id").value(otherUser.getId())).andReturn());

        asTeacher(delete(RULE, room.getId(), "FACILITY")).andExpect(status().isNoContent());
        mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").isEmpty());
        entityManager.flush();
        entityManager.clear();
        asTeacher(get("/api/posts/{id}", original)).andExpect(jsonPath("$.assignee.id").value(teacher.getId()));
        asTeacher(get("/api/posts/{id}", reassigned)).andExpect(jsonPath("$.assignee.id").value(otherUser.getId()));
    }

    @Test
    void listsOnlyEffectiveLegacyRuleAndDeletesOnlyItsRoomCategory() throws Exception {
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", otherUser.getId());
        assignTeacher(room, PostCategory.FACILITY, teacher);
        assignTeacher(room, PostCategory.FACILITY, otherUser);
        assignTeacher(room, PostCategory.NETWORK, otherUser);
        assignTeacher(samePlaceRoom, PostCategory.FACILITY, otherUser);
        entityManager.clear();

        String roomCategory = "$[?(@.roomId == %s && @.category == '%s')]";
        asTeacher(get(RULES))
                .andExpect(status().isOk())
                .andExpect(jsonPath(roomCategory, room.getId(), "FACILITY").value(hasSize(1)))
                .andExpect(jsonPath("$[?(@.roomId == %s && @.category == 'FACILITY' && @.teacherId == %s)]",
                        room.getId(), teacher.getId()).value(hasSize(1)));

        asTeacher(delete(RULE, room.getId(), "FACILITY")).andExpect(status().isNoContent());
        asTeacher(get(RULES))
                .andExpect(jsonPath(roomCategory, room.getId(), "FACILITY").isEmpty())
                .andExpect(jsonPath(roomCategory, room.getId(), "NETWORK").value(hasSize(1)))
                .andExpect(jsonPath(roomCategory, samePlaceRoom.getId(), "FACILITY").value(hasSize(1)));
        mvc.perform(createRequest(Map.of())).andExpect(jsonPath("$.assignee").isEmpty());
    }

    @Test
    void rejectsMissingRulesInvalidInputsAndStudentAssignees() throws Exception {
        asTeacher(update(teacher.getId())).andExpect(status().isNotFound());
        asTeacher(delete(RULE, room.getId(), "FACILITY")).andExpect(status().isNotFound());
        asTeacher(json(post(RULES), Map.of("roomId", room.getId(), "teacherId", teacher.getId())))
                .andExpect(status().isBadRequest());
        asTeacher(json(post(RULES), Map.of("roomId", Long.MAX_VALUE, "category", "FACILITY", "teacherId", teacher.getId())))
                .andExpect(status().isNotFound());
        asTeacher(create(Long.MAX_VALUE)).andExpect(status().isNotFound());
        asTeacher(create(author.getId())).andExpect(status().isConflict());
        asTeacher(create(teacher.getId())).andExpect(status().isCreated());
        asTeacher(update(author.getId())).andExpect(status().isConflict());
        mvc.perform(createRequest(Map.of())).andExpect(jsonPath("$.assignee.id").value(teacher.getId()));
    }

    @Test
    void restrictsAllRuleEndpointsToTeachers() throws Exception {
        for (MockHttpServletRequestBuilder request : List.of(get(RULES), create(teacher.getId()),
                update(teacher.getId()), delete(RULE, room.getId(), "FACILITY"))) {
            mvc.perform(request).andExpect(status().isUnauthorized());
            mvc.perform(request.with(user(CustomUserDetails.from(author)))).andExpect(status().isForbidden());
        }
    }

    private MockHttpServletRequestBuilder create(Long teacherId) {
        return json(post(RULES), Map.of("roomId", room.getId(), "category", "FACILITY", "teacherId", teacherId));
    }

    private MockHttpServletRequestBuilder update(Long teacherId) {
        return json(put(RULE, room.getId(), "FACILITY"), Map.of("teacherId", teacherId));
    }

    private MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, Map<String, ?> body) {
        return request.contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
    }

    private ResultActions asTeacher(MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.with(user(CustomUserDetails.from(teacher))));
    }
}
