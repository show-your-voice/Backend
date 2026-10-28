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
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.global.security.CustomUserDetails;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.web.servlet.MvcResult;

class PostCreationIntegrationTests extends PostApiTestSupport {

    @Test
    void createsAndReadsCompleteReportWhenTeacherIsAssigned() throws Exception {
        assignTeacher(place, PostCategory.FACILITY, teacher);
        LocalDateTime before = LocalDateTime.now();

        MvcResult result = mvc.perform(createRequest(Map.of(
                        "improvement", "수도꼭지를 교체해 주세요.",
                        "authorId", otherUser.getId(), "assigneeId", otherUser.getId(),
                        "status", "RESOLVED", "receivedAt", "2000-01-01T00:00:00")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("화장실 세면대 물이 안 나와요"))
                .andExpect(jsonPath("$.content").value("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다."))
                .andExpect(jsonPath("$.improvement").value("수도꼭지를 교체해 주세요."))
                .andExpect(jsonPath("$.author.id").value(author.getId()))
                .andExpect(jsonPath("$.author.name").value("김학생"))
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()))
                .andExpect(jsonPath("$.assignee.name").value("이선생"))
                .andExpect(jsonPath("$.place.name").value("창의관 4층"))
                .andExpect(jsonPath("$.place.imageUrl").value("https://example.com/creative-4f.png"))
                .andExpect(jsonPath("$.posX").value(0.4215))
                .andExpect(jsonPath("$.posY").value(0.653))
                .andExpect(jsonPath("$.category.code").value("FACILITY"))
                .andExpect(jsonPath("$.category.label").value("시설"))
                .andExpect(jsonPath("$.status.code").value("UNRESOLVED"))
                .andExpect(jsonPath("$.status.label").value("미해결"))
                .andExpect(jsonPath("$.inProgressAt").value(nullValue()))
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
        assertThat(persisted.getPosX()).isEqualByComparingTo("0.4215");
        assertThat(persisted.getPosY()).isEqualByComparingTo("0.653");
        assertThat(persisted.getAuthor().getId()).isEqualTo(author.getId());
        assertThat(persisted.getAssignee().getId()).isEqualTo(teacher.getId());
        assertThat(persisted.getStatus()).isEqualTo(PostStatus.UNRESOLVED);
        assertThat(persisted.getCreatedAt()).isBetween(before, LocalDateTime.now());

        mvc.perform(get("/api/posts/{id}", postId).with(user(CustomUserDetails.from(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("화장실 세면대 물이 안 나와요"))
                .andExpect(jsonPath("$.content").value("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다."))
                .andExpect(jsonPath("$.improvement").value("수도꼭지를 교체해 주세요."))
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()))
                .andExpect(jsonPath("$.author.email").value(author.getEmail()))
                .andExpect(jsonPath("$.receivedAt").isNotEmpty())
                .andExpect(jsonPath("$.author.password").doesNotExist());
    }

    @Test
    void readsAttachedImagesInSortOrder() throws Exception {
        long id = responseId(mvc.perform(createRequest(Map.of()))
                .andExpect(jsonPath("$.imageUrls").isEmpty()).andReturn());
        jdbc.update("""
                INSERT INTO tbl_post_image (post_id, image_url, sort_order)
                VALUES (?, 'https://example.com/second.jpg', 1), (?, 'https://example.com/first.jpg', 0)
                """, id, id);

        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls[0]").value("https://example.com/first.jpg"))
                .andExpect(jsonPath("$.imageUrls[1]").value("https://example.com/second.jpg"));
    }

    @Test
    void createsUnassignedReportWhenOptionalFieldsAndPlaceMappingAreAbsent() throws Exception {
        assignTeacher(otherPlace, PostCategory.FACILITY, teacher);

        mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").value(nullValue()))
                .andExpect(jsonPath("$.improvement").value(nullValue()))
                .andExpect(jsonPath("$.status.code").value("UNRESOLVED"));
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
    void assignsTeacherOfMatchingPlaceAndCategoryRule(PostCategory category) throws Exception {
        assignTeacher(place, category, teacher);

        mvc.perform(createRequest(Map.of("category", category.name())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()));
    }

    @Test
    void leavesUnassignedWhenOnlyOtherCategoryRuleExistsForPlace() throws Exception {
        assignTeacher(place, PostCategory.NETWORK, teacher);

        mvc.perform(createRequest(Map.of("category", "FACILITY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee").value(nullValue()));
    }

    @Test
    void assignsFirstRegisteredTeacherWhenRulesOverlap() throws Exception {
        User laterTeacher = userRepository.save(
                User.createStudent("later" + UUID.randomUUID().toString().substring(0, 8),
                        "later-" + UUID.randomUUID() + "@dsm.hs.kr", "unused", "박선생"));
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", laterTeacher.getId());
        assignTeacher(place, PostCategory.FACILITY, teacher);
        assignTeacher(place, PostCategory.FACILITY, laterTeacher);
        entityManager.clear();

        mvc.perform(createRequest(Map.of("category", "FACILITY")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()));
    }

    @Test
    void keepsOriginalAssigneeWhenPlaceMappingChangesAfterCreation() throws Exception {
        assignTeacher(place, PostCategory.FACILITY, teacher);
        long id = responseId(mvc.perform(createRequest(Map.of())).andExpect(status().isCreated()).andReturn());
        jdbc.update("DELETE FROM tbl_assignment_rule WHERE place_id = ?", place.getId());
        entityManager.clear();

        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(author))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee.id").value(teacher.getId()));
    }

    @Test
    void rejectsStudentMappingWithoutPersistingPostWhenDatabaseMappingIsInvalid() throws Exception {
        assignTeacher(place, PostCategory.FACILITY, otherUser);
        long count = postRepository.count();

        mvc.perform(createRequest(Map.of()))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("L002"));

        assertThat(postRepository.count()).isEqualTo(count);
    }
}
