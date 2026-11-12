package com.example.showyourvoice.domain.post.service;

import com.example.showyourvoice.domain.post.domain.PostCreatedEvent;
import com.example.showyourvoice.global.config.MailNotificationProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class PostMailNotifier {

    private final JavaMailSender mailSender;
    private final MailNotificationProperties properties;

    // 게시글이 커밋된 뒤에만 별도 스레드에서 보낸다. 발송이 실패해도 이미 끝난 등록 결과에는 영향이 없다.
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, condition = "#event.hasAssignee()")
    public void notifyAssignee(PostCreatedEvent event) {
        try {
            mailSender.send(message(event));
        } catch (MailException e) {
            log.warn("게시글 {} 담당 선생님({}) 알림 메일 발송 실패", event.postId(), event.assigneeId(), e);
        }
    }

    private SimpleMailMessage message(PostCreatedEvent event) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(properties.from());
        message.setTo(event.assigneeEmail());
        message.setSubject("[너목보] 새 불편 신고: " + event.title());
        message.setText("""
                %s 선생님, 담당하신 위치에 새 불편 신고가 등록되었습니다.

                제목: %s
                위치: %s %s

                내용:
                %s

                상세 보기: %s
                """.formatted(event.assigneeName(), event.title(), event.placeName(), event.roomName(),
                event.content(), properties.managerReportUrl(event.postId())));
        return message;
    }
}
