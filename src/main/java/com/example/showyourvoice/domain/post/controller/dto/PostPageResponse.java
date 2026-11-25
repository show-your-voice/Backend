package com.example.showyourvoice.domain.post.controller.dto;

import com.example.showyourvoice.domain.post.domain.Post;
import java.util.List;
import org.springframework.data.domain.Page;

public record PostPageResponse(
        List<PostSummaryResponse> posts,
        int page,
        int size,
        long totalCount,
        int totalPages
) {

    public static PostPageResponse from(Page<Post> page) {
        return new PostPageResponse(
                page.map(PostSummaryResponse::from).getContent(),
                page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }
}
