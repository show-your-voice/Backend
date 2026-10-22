package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import java.time.LocalDateTime;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String improvement,
        CategoryInfo category,
        PlaceInfoResponse place,
        UserInfoResponse reporter,
        UserInfoResponse assignedTeacher,
        StatusInfo status,
        LocalDateTime receivedAt,
        LocalDateTime processingStartedAt,
        LocalDateTime resolvedAt
) {

    public static PostDetailResponse from(Post post) {
        return new PostDetailResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getImprovement(),
                new CategoryInfo(post.getCategory(), post.getCategory().getLabel()),
                PlaceInfoResponse.from(post.getPlace()), UserInfoResponse.from(post.getReporter()),
                post.getAssignedTeacher() == null ? null : UserInfoResponse.from(post.getAssignedTeacher()),
                new StatusInfo(post.getStatus(), post.getStatus().getLabel()),
                post.getCreatedAt(), post.getProcessingStartedAt(), post.getResolvedAt());
    }

    public record CategoryInfo(PostCategory code, String label) {
    }

    public record StatusInfo(PostStatus code, String label) {
    }
}
