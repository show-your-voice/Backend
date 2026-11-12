package com.example.showyourvoice.domain.post.domain;

import com.example.showyourvoice.domain.user.domain.User;

public record PostCreatedEvent(
        Long postId,
        String title,
        String content,
        String placeName,
        String roomName,
        Long assigneeId,
        String assigneeName,
        String assigneeEmail
) {

    // 커밋 뒤 다른 스레드에서 쓰므로, 지연 로딩 엔티티 대신 트랜잭션 안에서 필요한 값을 꺼내 둔다.
    public static PostCreatedEvent from(Post post) {
        User assignee = post.getAssignee();
        return new PostCreatedEvent(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getRoom().getPlace().getName(),
                post.getRoom().getName(),
                assignee == null ? null : assignee.getId(),
                assignee == null ? null : assignee.getName(),
                assignee == null ? null : assignee.getEmail()
        );
    }

    public boolean hasAssignee() {
        return assigneeEmail != null;
    }
}
