package com.example.showyourvoice.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.global.security.CustomUserDetails;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@Transactional
class ManagerListIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private UserRepository userRepository;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private EntityManager entityManager;

    private User student;

    @BeforeEach
    void prepareStudent() {
        student = userRepository.save(User.createStudent("manager-reader-" + UUID.randomUUID() + "@dsm.hs.kr",
                "unused", "김학생"));
    }

    @Test
    void listsTeachersWithDistinctLocationsAndOnlyPublicFields() throws Exception {
        User teacher = createTeacher("이선생");
        User unassigned = createTeacher("김선생");
        Place readingRoom = createPlace("4층 독서실");
        Place restroom = createPlace("4층 화장실(남)");
        assign(restroom, PostCategory.FACILITY, teacher);
        assign(readingRoom, PostCategory.FACILITY, teacher);
        assign(readingRoom, PostCategory.NETWORK, teacher);
        assign(readingRoom, PostCategory.ENVIRONMENT, student);
        entityManager.clear();

        List<Map<String, Object>> managers = getManagers(student);

        assertThat(findManager(managers, teacher))
                .containsOnlyKeys("id", "name", "email", "locations")
                .containsEntry("name", "이선생")
                .containsEntry("email", teacher.getEmail())
                .containsEntry("locations", List.of("4층 독서실", "4층 화장실(남)"));
        assertThat(findManager(managers, unassigned)).containsEntry("locations", List.of());
        assertThat(managers).noneMatch(manager -> idOf(manager).equals(student.getId()));
    }

    @Test
    void keepsSameNamedPlacesIndependentAndDeduplicatesTheirDisplayNames() throws Exception {
        User first = createTeacher("이선생");
        User second = createTeacher("김선생");
        String name = "4층 독서실";
        assign(createPlace(name), PostCategory.FACILITY, first);
        assign(createPlace(name), PostCategory.FACILITY, first);
        assign(createPlace(name), PostCategory.FACILITY, second);

        List<Map<String, Object>> managers = getManagers(student);

        assertThat(findManager(managers, first)).containsEntry("locations", List.of(name));
        assertThat(findManager(managers, second)).containsEntry("locations", List.of(name));
    }

    @Test
    void showsOnlyFirstRuleForEachPlaceAndCategory() throws Exception {
        User first = createTeacher("먼저 등록된 선생님");
        User later = createTeacher("나중에 등록된 선생님");
        Place place = createPlace("4층 독서실");
        assign(place, PostCategory.FACILITY, first);
        assign(place, PostCategory.FACILITY, later);

        List<Map<String, Object>> managers = getManagers(student);
        assertThat(findManager(managers, first)).containsEntry("locations", List.of(place.getName()));
        assertThat(findManager(managers, later)).containsEntry("locations", List.of());

        assign(place, PostCategory.NETWORK, later);
        managers = getManagers(student);
        assertThat(findManager(managers, later)).containsEntry("locations", List.of(place.getName()));
    }

    @Test
    void doesNotTreatLaterTeacherRuleAsEffectiveWhenFirstRuleHasStudent() throws Exception {
        User teacher = createTeacher("이선생");
        Place place = createPlace("4층 독서실");
        assign(place, PostCategory.FACILITY, student);
        assign(place, PostCategory.FACILITY, teacher);

        assertThat(findManager(getManagers(student), teacher)).containsEntry("locations", List.of());
    }

    @Test
    void sortsTeachersByNameThenIdAndAllowsTeacherReaders() throws Exception {
        User laterName = createTeacher("하선생");
        User firstSameName = createTeacher("가선생");
        User secondSameName = createTeacher("가선생");

        List<Long> fixtureIds = List.of(laterName.getId(), firstSameName.getId(), secondSameName.getId());
        List<Long> sortedIds = getManagers(laterName).stream()
                .map(ManagerListIntegrationTests::idOf)
                .filter(fixtureIds::contains)
                .toList();

        assertThat(sortedIds).containsExactly(firstSameName.getId(), secondSameName.getId(), laterName.getId());
    }

    @Test
    void loadsMultipleTeachersAndPlacesInTwoQueries() throws Exception {
        for (int index = 0; index < 5; index++) {
            User teacher = createTeacher("담당 선생님 " + index);
            assign(createPlace("4층 교실 " + index), PostCategory.FACILITY, teacher);
            assign(createPlace("4층 실습실 " + index), PostCategory.NETWORK, teacher);
            User nonTeacher = userRepository.save(User.createStudent("non-teacher-" + UUID.randomUUID() + "@dsm.hs.kr",
                    "unused", "학생 " + index));
            assign(createPlace("4층 기타 장소 " + index), PostCategory.FACILITY, nonTeacher);
        }
        entityManager.flush();
        entityManager.clear();
        Statistics statistics = entityManager.getEntityManagerFactory().unwrap(SessionFactory.class).getStatistics();
        boolean enabled = statistics.isStatisticsEnabled();
        statistics.setStatisticsEnabled(true);
        statistics.clear();

        try {
            getManagers(student);
            assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
        } finally {
            statistics.setStatisticsEnabled(enabled);
        }
    }

    @Test
    void rejectsAnonymousRequests() throws Exception {
        mvc.perform(get("/api/managers"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("A001"));
    }

    private User createTeacher(String name) {
        User teacher = userRepository.save(User.createStudent("manager-" + UUID.randomUUID() + "@dsm.hs.kr",
                "unused", name));
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", teacher.getId());
        entityManager.detach(teacher);
        return userRepository.findById(teacher.getId()).orElseThrow();
    }

    private Place createPlace(String name) {
        return placeRepository.save(Place.builder().name(name).imageUrl("https://example.com/4f.png").build());
    }

    private void assign(Place place, PostCategory category, User teacher) {
        jdbc.update("""
                INSERT INTO tbl_assignment_rule (place_id, teacher_id, category, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, place.getId(), teacher.getId(), category.name());
    }

    private List<Map<String, Object>> getManagers(User reader) throws Exception {
        String response = mvc.perform(get("/api/managers").with(user(CustomUserDetails.from(reader))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return JsonPath.read(response, "$");
    }

    private static Map<String, Object> findManager(List<Map<String, Object>> managers, User teacher) {
        return managers.stream().filter(manager -> idOf(manager).equals(teacher.getId())).findFirst().orElseThrow();
    }

    private static Long idOf(Map<String, Object> manager) {
        return ((Number) manager.get("id")).longValue();
    }
}
