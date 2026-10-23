package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.place.controller.dto.PlaceInfoResponse;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.post.domain.PostImage;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
        Long id,
        String title,
        String content,
        String improvement,
        CategoryInfo category,
        PlaceInfoResponse place,
        BigDecimal posX,
        BigDecimal posY,
        UserInfoResponse author,
        UserInfoResponse assignee,
        StatusInfo status,
        List<String> imageUrls,
        LocalDateTime receivedAt,
        LocalDateTime inProgressAt,
        LocalDateTime resolvedAt
) {

    public static PostDetailResponse of(Post post, List<PostImage> images) {
        return new PostDetailResponse(
                post.getId(), post.getTitle(), post.getContent(), post.getImprovement(),
                new CategoryInfo(post.getCategory(), post.getCategory().getLabel()),
                PlaceInfoResponse.from(post.getPlace()), post.getPosX(), post.getPosY(),
                UserInfoResponse.from(post.getAuthor()),
                post.getAssignee() == null ? null : UserInfoResponse.from(post.getAssignee()),
                new StatusInfo(post.getStatus(), post.getStatus().getLabel()),
                images.stream().map(PostImage::getImageUrl).toList(),
                post.getCreatedAt(), post.getInProgressAt(), post.getResolvedAt());
    }

    public record CategoryInfo(PostCategory code, String label) {
    }

    public record StatusInfo(PostStatus code, String label) {
    }
}
