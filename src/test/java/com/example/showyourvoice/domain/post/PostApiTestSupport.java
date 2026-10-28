package com.example.showyourvoice.domain.post;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.global.security.CustomUserDetails;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "spring.jpa.open-in-view=false")
@AutoConfigureMockMvc
@Transactional
abstract class PostApiTestSupport {

    @Autowired protected MockMvc mvc;
    @Autowired protected JsonMapper jsonMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected PlaceRepository placeRepository;
    @Autowired protected PostRepository postRepository;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired protected EntityManager entityManager;

    protected User author;
    protected User otherUser;
    protected User teacher;
    protected Place place;
    protected Place otherPlace;

    @BeforeEach
    void prepareFixtures() {
        String suffix = UUID.randomUUID().toString();
        String idSuffix = suffix.substring(0, 8);
        author = userRepository.save(User.createStudent("author" + idSuffix, "author-" + suffix + "@dsm.hs.kr", "unused", "김학생"));
        otherUser = userRepository.save(User.createStudent("other" + idSuffix, "other-" + suffix + "@dsm.hs.kr", "unused", "다른학생"));
        teacher = userRepository.save(User.createStudent("teacher" + idSuffix, "teacher-" + suffix + "@dsm.hs.kr", "unused", "이선생"));
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", teacher.getId());
        entityManager.clear();
        teacher = userRepository.findById(teacher.getId()).orElseThrow();
        place = placeRepository.save(Place.builder().name("창의관 4층").imageUrl("https://example.com/creative-4f.png").build());
        otherPlace = placeRepository.save(Place.builder().name("기숙사 2층").imageUrl("https://example.com/dorm-2f.png").build());
    }

    protected void assignTeacher(Place target, PostCategory category, User assignee) {
        jdbc.update("""
                INSERT INTO tbl_assignment_rule (place_id, teacher_id, category, created_at, updated_at)
                VALUES (?, ?, ?, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, target.getId(), assignee.getId(), category.name());
    }

    protected MockHttpServletRequestBuilder createRequest(Map<String, Object> overrides) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "화장실 세면대 물이 안 나와요");
        body.put("content", "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.");
        body.put("category", "FACILITY");
        body.put("placeId", place.getId());
        body.put("posX", 0.4215);
        body.put("posY", 0.653);
        body.putAll(overrides);
        return post("/api/posts").with(user(CustomUserDetails.from(author)))
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
    }

    protected static long responseId(MvcResult result) throws Exception {
        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.id");
        return id.longValue();
    }

}
