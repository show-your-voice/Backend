package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.after;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.global.security.CustomUserDetails;
import com.jayway.jsonpath.JsonPath;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.json.JsonMapper;

// 메일은 게시글이 커밋된 뒤에만 나가므로 테스트 트랜잭션을 쓰지 않고, 커밋된 데이터를 테스트가 끝날 때 직접 지운다.
@SpringBootTest(properties = {
        "spring.jpa.open-in-view=false",
        "notification.mail.from=noreply@show-your-voice.test",
        "notification.mail.frontend-url=https://front.example/"
})
@AutoConfigureMockMvc
class PostMailNotificationIntegrationTests {

    @MockitoBean private JavaMailSender mailSender;
    @Autowired private MockMvc mvc;
    @Autowired private JsonMapper jsonMapper;
    @Autowired private UserRepository userRepository;
    @Autowired private PlaceRepository placeRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private PostRepository postRepository;
    @Autowired private JdbcTemplate jdbc;

    private User author;
    private User teacher;
    private Place place;
    private Room room;

    @BeforeEach
    void prepareCommittedFixtures() {
        String suffix = UUID.randomUUID().toString();
        String idSuffix = suffix.substring(0, 8);
        author = userRepository.save(User.createStudent("mailauthor" + idSuffix, "mail-author-" + suffix + "@dsm.hs.kr", "unused", "김학생"));
        teacher = userRepository.save(User.createStudent("mailteacher" + idSuffix, "mail-teacher-" + suffix + "@dsm.hs.kr", "unused", "이선생"));
        jdbc.update("UPDATE tbl_user SET role = 'TEACHER' WHERE id = ?", teacher.getId());
        place = placeRepository.save(Place.builder().name("창의관 4층").imageUrl("https://example.com/creative-4f.png").build());
        room = roomRepository.save(Room.builder().name("화장실(남)").place(place).build());
    }

    @AfterEach
    void removeCommittedFixtures() {
        jdbc.update("DELETE FROM tbl_post WHERE author_id = ?", author.getId());
        jdbc.update("DELETE FROM tbl_assignment_rule WHERE room_id = ?", room.getId());
        roomRepository.deleteById(room.getId());
        placeRepository.deleteById(place.getId());
        userRepository.deleteById(teacher.getId());
        userRepository.deleteById(author.getId());
    }

    @Test
    void sendsReportSummaryAndManagerLinkToAssignedTeacher() throws Exception {
        assignTeacherToRoom();

        long postId = createPost();

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender, timeout(5000)).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly(teacher.getEmail());
        assertThat(message.getFrom()).isEqualTo("noreply@show-your-voice.test");
        assertThat(message.getSubject()).isEqualTo("[너목보] 새 불편 신고: 화장실 세면대 물이 안 나와요");
        assertThat(message.getText())
                .contains("이선생 선생님")
                .contains("제목: 화장실 세면대 물이 안 나와요")
                .contains("위치: 창의관 4층 화장실(남)")
                .contains("2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.")
                .contains("상세 보기: https://front.example/manager/reports/" + postId);
    }

    @Test
    void respondsWithoutWaitingForMailDelivery() throws Exception {
        assignTeacherToRoom();
        String requestThread = Thread.currentThread().getName();
        CountDownLatch releaseDelivery = new CountDownLatch(1);
        CompletableFuture<String> deliveryThread = new CompletableFuture<>();
        doAnswer(invocation -> {
            releaseDelivery.await(5, TimeUnit.SECONDS);
            deliveryThread.complete(Thread.currentThread().getName());
            return null;
        }).when(mailSender).send(any(SimpleMailMessage.class));

        createPost();

        assertThat(deliveryThread).isNotDone();
        releaseDelivery.countDown();
        assertThat(deliveryThread.get(5, TimeUnit.SECONDS)).isNotEqualTo(requestThread);
    }

    @Test
    void keepsCreatedReportWhenMailDeliveryFails() throws Exception {
        assignTeacherToRoom();
        doThrow(new MailSendException("SMTP 연결 실패")).when(mailSender).send(any(SimpleMailMessage.class));

        long postId = createPost();

        verify(mailSender, timeout(5000)).send(any(SimpleMailMessage.class));
        assertThat(postRepository.findById(postId)).isPresent();
    }

    @Test
    void doesNotSendMailWhenNoTeacherIsAssigned() throws Exception {
        createPost();

        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void doesNotSendMailWhenReportIsRejected() throws Exception {
        assignTeacherToRoom();

        mvc.perform(createRequest(Map.of("imageUrls", List.of("https://example.com/outside.jpg"))))
                .andExpect(status().isBadRequest());

        verify(mailSender, after(1000).never()).send(any(SimpleMailMessage.class));
    }

    private void assignTeacherToRoom() {
        jdbc.update("""
                INSERT INTO tbl_assignment_rule (room_id, teacher_id, category, created_at, updated_at)
                VALUES (?, ?, 'FACILITY', CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6))
                """, room.getId(), teacher.getId());
    }

    private long createPost() throws Exception {
        String response = mvc.perform(createRequest(Map.of()))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(response, "$.id");
        return id.longValue();
    }

    private MockHttpServletRequestBuilder createRequest(Map<String, Object> overrides) throws Exception {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "화장실 세면대 물이 안 나와요");
        body.put("content", "2번째 세면대 수도꼭지를 돌려도 물이 나오지 않습니다.");
        body.put("category", "FACILITY");
        body.put("roomId", room.getId());
        body.put("posX", 0.4215);
        body.put("posY", 0.653);
        body.putAll(overrides);
        return post("/api/posts").with(user(CustomUserDetails.from(author)))
                .contentType(MediaType.APPLICATION_JSON).content(jsonMapper.writeValueAsString(body));
    }
}
