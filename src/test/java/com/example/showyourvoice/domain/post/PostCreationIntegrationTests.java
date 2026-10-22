package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

class PostCreationIntegrationTests extends PostApiTestSupport {

    @Test
    void createsAndReadsCompleteReportWhenTeacherIsAssigned() throws Exception {
        assignTeacher(place, teacher);
        LocalDateTime before = LocalDateTime.now();

        MvcResult result = mvc.perform(createRequest(Map.of(
                        "improvement", "수도꼭지를 교체해 주세요.",
                        "reporterId", otherUser.getId(), "assignedTeacherId", otherUser.getId(),
                        "status", "COMPLETED", "receivedAt", "2000-01-01T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("화장실 세면대 물이 안 나와요"))
                .andExpect(jsonPath("$.content").value("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다."))
                .andExpect(jsonPath("$.improvement").value("수도꼭지를 교체해 주세요."))
                .andExpect(jsonPath("$.reporter.id").value(reporter.getId()))
                .andExpect(jsonPath("$.reporter.name").value("김학생"))
                .andExpect(jsonPath("$.assignedTeacher.id").value(teacher.getId()))
                .andExpect(jsonPath("$.assignedTeacher.name").value("이선생"))
                .andExpect(jsonPath("$.place.buildingName").value("창의관"))
                .andExpect(jsonPath("$.place.floor").value(4))
                .andExpect(jsonPath("$.place.name").value("화장실(남)"))
                .andExpect(jsonPath("$.category.code").value("FACILITY"))
                .andExpect(jsonPath("$.category.label").value("시설 고장"))
                .andExpect(jsonPath("$.status.code").value("RECEIVED"))
                .andExpect(jsonPath("$.status.label").value("접수"))
                .andExpect(jsonPath("$.processingStartedAt").value(nullValue()))
                .andExpect(jsonPath("$.resolvedAt").value(nullValue()))
                .andExpect(jsonPath("$.receivedAt").isNotEmpty())
                .andReturn();
        long postId = responseId(result);
        assertThat(result.getResponse().getHeader("Location")).isEqualTo("/api/posts/" + postId);
        entityManager.flush();
        entityManager.clear();
        Post persisted = postRepository.findById(postId).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo("화장실 세면대 물이 안 나와요");
        assertThat(persisted.getContent()).isEqualTo("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.");
        assertThat(persisted.getImprovement()).isEqualTo("수도꼭지를 교체해 주세요.");
        assertThat(persisted.getCategory()).isEqualTo(PostCategory.FACILITY);
        assertThat(persisted.getPlace().getId()).isEqualTo(place.getId());
        assertThat(persisted.getReporter().getId()).isEqualTo(reporter.getId());
        assertThat(persisted.getAssignedTeacher().getId()).isEqualTo(teacher.getId());
        assertThat(persisted.getStatus()).isEqualTo(PostStatus.RECEIVED);
        assertThat(persisted.getCreatedAt()).isBetween(before, LocalDateTime.now());

        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("화장실 세면대 물이 안 나와요"))
                .andExpect(jsonPath("$.content").value("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다."))
                .andExpect(jsonPath("$.improvement").value("수도꼭지를 교체해 주세요."))
                .andExpect(jsonPath("$.assignedTeacher.id").value(teacher.getId()))
                .andExpect(jsonPath("$.reporter.email").value(reporter.getEmail()))
                .andExpect(jsonPath("$.receivedAt").isNotEmpty())
                .andExpect(jsonPath("$.reporter.password").doesNotExist());
    }

    @Test
    void createsUnassignedReportWhenOptionalFieldsAndPlaceMappingAreAbsent() throws Exception {
        assignTeacher(otherPlace, teacher);

        mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedTeacher").value(nullValue()))
                .andExpect(jsonPath("$.improvement").value(nullValue()))
                .andExpect(jsonPath("$.status.code").value("RECEIVED"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void storesNullImprovementWhenOptionalValueIsEmpty(String improvement) throws Exception {
        long id = responseId(mvc.perform(createRequest(Collections.singletonMap("improvement", improvement)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.improvement").value(nullValue())).andReturn());
        entityManager.flush();
        entityManager.clear();

        assertThat(postRepository.findById(id).orElseThrow().getImprovement()).isNull();
    }

    @Test
    void preservesFieldsAtMaximumAllowedLengths() throws Exception {
        String title = "가".repeat(100);
        String content = "나".repeat(5000);
        String improvement = "다".repeat(1000);
        long id = responseId(mvc.perform(createRequest(Map.of(
                        "title", title, "content", content, "improvement", improvement)))
                .andExpect(status().isCreated()).andReturn());
        entityManager.flush();
        entityManager.clear();

        Post persisted = postRepository.findById(id).orElseThrow();
        assertThat(persisted.getTitle()).isEqualTo(title);
        assertThat(persisted.getContent()).isEqualTo(content);
        assertThat(persisted.getImprovement()).isEqualTo(improvement);
    }

    @ParameterizedTest
    @EnumSource(PostCategory.class)
    void assignsPlaceTeacherRegardlessOfCategoryWhenMappingExists(PostCategory category) throws Exception {
        assignTeacher(place, teacher);

        mvc.perform(createRequest(Map.of("category", category.name())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignedTeacher.id").value(teacher.getId()));
    }

    @Test
    void keepsOriginalAssigneeWhenPlaceMappingChangesAfterCreation() throws Exception {
        assignTeacher(place, teacher);
        long id = responseId(mvc.perform(createRequest(Map.of())).andExpect(status().isCreated()).andReturn());
        jdbc.update("DELETE FROM tbl_place_teacher_assignment WHERE place_id = ?", place.getId());
        entityManager.clear();

        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(reporter))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignedTeacher.id").value(teacher.getId()));
    }

    @Test
    void rejectsStudentMappingWithoutPersistingPostWhenDatabaseMappingIsInvalid() throws Exception {
        assignTeacher(place, otherUser);
        long count = postRepository.count();

        mvc.perform(createRequest(Map.of()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("L002"));

        assertThat(postRepository.count()).isEqualTo(count);
    }
}
