package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import java.net.CookieManager;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.jpa.open-in-view=false")
class PostHttpIntegrationTests {

    @Autowired private UserRepository userRepository;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JsonMapper jsonMapper;
    @Value("${local.server.port}") private int port;

    private HttpClient client;
    private User author;
    private User teacher;
    private Place place;

    @BeforeEach
    void prepareCommittedFixturesAndLogin() throws Exception {
        String suffix = UUID.randomUUID().toString();
        author = userRepository.save(User.createStudent("http-author-" + suffix + "@dsm.hs.kr",
                passwordEncoder.encode("LocalTest123!"), "김학생"));
        teacher = userRepository.save(User.createStudent("http-teacher-" + suffix + "@dsm.hs.kr", "unused", "이선생"));
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", teacher.getId());
        place = placeRepository.save(Place.builder().name("창의관 4층").imageUrl("https://example.com/creative-4f.png").build());
        jdbc.update("""
                INSERT INTO tbl_assignment_rule (place_id, teacher_id, category, created_at, updated_at)
                VALUES (?, ?, 'FACILITY', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, place.getId(), teacher.getId());
        client = HttpClient.newBuilder().cookieHandler(new CookieManager())
                .connectTimeout(Duration.ofSeconds(5)).build();

        HttpResponse<String> login = post("/api/auth/login",
                Map.of("email", author.getEmail(), "password", "LocalTest123!"));
        assertThat(login.statusCode()).isEqualTo(200);
    }

    @AfterEach
    void removeOnlyCommittedTestFixtures() {
        if (client != null) {
            client.close();
        }
        if (author != null) {
            jdbc.update("DELETE FROM tbl_post WHERE author_id = ?", author.getId());
        }
        if (place != null) {
            jdbc.update("DELETE FROM tbl_assignment_rule WHERE place_id = ?", place.getId());
            placeRepository.deleteById(place.getId());
        }
        if (teacher != null) {
            userRepository.deleteById(teacher.getId());
        }
        if (author != null) {
            userRepository.deleteById(author.getId());
        }
    }

    @Test
    void commitsAllFieldsAndReturnsSameDetailThroughHttp() throws Exception {
        LocalDateTime before = LocalDateTime.now();
        HttpResponse<String> response = post("/api/posts", Map.ofEntries(
                Map.entry("title", "화장실 세면대 물이 안 나와요"), Map.entry("content", "수도꼭지를 돌려도 물이 나오지 않습니다."),
                Map.entry("category", "FACILITY"), Map.entry("placeId", place.getId()),
                Map.entry("posX", 0.4215), Map.entry("posY", 0.653), Map.entry("improvement", "수도꼭지를 교체해 주세요."),
                Map.entry("authorId", teacher.getId()), Map.entry("assigneeId", author.getId()),
                Map.entry("status", "RESOLVED"), Map.entry("receivedAt", "2000-01-01T00:00:00")));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode created = jsonMapper.readTree(response.body());
        long id = created.get("id").asLong();
        String location = response.headers().firstValue("Location").orElseThrow();
        assertThat(location).isEqualTo("/api/posts/" + id);
        assertThat(created.get("title").asString()).isEqualTo("화장실 세면대 물이 안 나와요");
        assertThat(created.get("content").asString()).isEqualTo("수도꼭지를 돌려도 물이 나오지 않습니다.");
        assertThat(created.get("improvement").asString()).isEqualTo("수도꼭지를 교체해 주세요.");
        assertThat(created.at("/category/code").asString()).isEqualTo("FACILITY");
        assertThat(created.at("/category/label").asString()).isEqualTo("시설");
        assertThat(created.at("/place/id").asLong()).isEqualTo(place.getId());
        assertThat(created.at("/place/name").asString()).isEqualTo("창의관 4층");
        assertThat(created.at("/place/imageUrl").asString()).isEqualTo("https://example.com/creative-4f.png");
        assertThat(created.get("posX").decimalValue()).isEqualByComparingTo("0.4215");
        assertThat(created.get("posY").decimalValue()).isEqualByComparingTo("0.653");
        assertThat(created.at("/author/id").asLong()).isEqualTo(author.getId());
        assertThat(created.at("/author/name").asString()).isEqualTo("김학생");
        assertThat(created.at("/assignee/id").asLong()).isEqualTo(teacher.getId());
        assertThat(created.at("/assignee/role").asString()).isEqualTo("TEACHER");
        assertThat(created.at("/status/code").asString()).isEqualTo("UNRESOLVED");
        assertThat(created.at("/status/label").asString()).isEqualTo("미해결");
        assertThat(LocalDateTime.parse(created.get("receivedAt").asString())).isBetween(before, LocalDateTime.now());
        assertThat(created.get("inProgressAt").isNull()).isTrue();
        assertThat(created.get("resolvedAt").isNull()).isTrue();

        Map<String, Object> stored = jdbc.queryForMap("SELECT * FROM tbl_post WHERE id = ?", id);
        assertThat(stored).containsEntry("title", "화장실 세면대 물이 안 나와요")
                .containsEntry("content", "수도꼭지를 돌려도 물이 나오지 않습니다.")
                .containsEntry("improvement", "수도꼭지를 교체해 주세요.")
                .containsEntry("category", "FACILITY").containsEntry("status", "UNRESOLVED")
                .containsEntry("place_id", place.getId()).containsEntry("author_id", author.getId())
                .containsEntry("assignee_id", teacher.getId());
        assertThat(stored.get("created_at")).isNotNull();

        HttpResponse<String> detail = client.send(request(location).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(detail.statusCode()).isEqualTo(200);
        assertThat(jsonMapper.readTree(detail.body())).isEqualTo(created);
    }

    @Test
    void commitsOptionalImprovementAndUnassignedTeacherAsNullThroughHttp() throws Exception {
        jdbc.update("DELETE FROM tbl_assignment_rule WHERE place_id = ?", place.getId());

        HttpResponse<String> response = post("/api/posts", Map.of(
                "title", "와이파이 연결이 끊겨요", "content", "인터넷 연결을 확인해 주세요.",
                "category", "NETWORK", "placeId", place.getId(), "posX", 0.1, "posY", 0.2));

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode created = jsonMapper.readTree(response.body());
        assertThat(created.get("improvement").isNull()).isTrue();
        assertThat(created.get("assignee").isNull()).isTrue();
        Map<String, Object> stored = jdbc.queryForMap("SELECT improvement, assignee_id FROM tbl_post WHERE id = ?",
                created.get("id").asLong());
        assertThat(stored).containsEntry("improvement", null).containsEntry("assignee_id", null);
    }

    private HttpResponse<String> post(String path, Map<String, Object> body) throws Exception {
        return client.send(request(path).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonMapper.writeValueAsString(body))).build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpRequest.Builder request(String path) {
        return HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).timeout(Duration.ofSeconds(10));
    }
}
