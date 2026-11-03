package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse.StatusInfo;
import com.example.showyourvoice.domain.post.domain.Post;
import java.time.LocalDateTime;

public record PostSummaryResponse(
        Long id,
        String title,
        String content,
        StatusInfo status,
        String authorName,
        String placeName,
        Long roomId,
        String roomName,
        LocalDateTime receivedAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
                post.getId(), post.getTitle(), post.getContent(),
                new StatusInfo(post.getStatus(), post.getStatus().getLabel()),
                post.getAuthor().getName(), post.getRoom().getPlace().getName(),
                post.getRoom().getId(), post.getRoom().getName(), post.getCreatedAt());
    }
}
