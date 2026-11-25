package com.example.showyourvoice.domain.post.controller;

import com.example.showyourvoice.domain.post.controller.dto.AddPostCoAssigneeRequest;
import com.example.showyourvoice.domain.post.controller.dto.ChangePostStatusRequest;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostPageResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostSummaryResponse;
import com.example.showyourvoice.domain.post.controller.dto.SavePostRequest;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.post.service.PostService;
import com.example.showyourvoice.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public ResponseEntity<PostDetailResponse> createPost(@Valid @RequestBody SavePostRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostDetailResponse response = postService.createPost(request, userDetails.getUserId());
        return ResponseEntity.created(URI.create("/api/posts/" + response.id())).body(response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> updatePost(@PathVariable Long postId,
                                                        @Valid @RequestBody SavePostRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.updatePost(postId, request, userDetails.getUserId(), userDetails.getRole()));
    }

    @GetMapping
    public ResponseEntity<PostPageResponse> getPosts(@RequestParam(required = false) Long placeId,
                                                     @RequestParam(required = false) PostStatus status,
                                                     @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(postService.getPosts(placeId, status, pageable));
    }

    @GetMapping("/assigned")
    public ResponseEntity<List<PostSummaryResponse>> getAssignedPosts(
            @RequestParam(required = false) PostStatus status,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.getAssignedPosts(userDetails.getUserId(), status));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long postId,
                                                      @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.getPost(postId, userDetails.getUserId(), userDetails.getRole()));
    }

    @PatchMapping("/{postId}/status")
    public ResponseEntity<PostDetailResponse> changeStatus(@PathVariable Long postId,
                                                           @Valid @RequestBody ChangePostStatusRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.changeStatus(
                postId, request.status(), userDetails.getUserId(), userDetails.getRole()));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{postId}/co-assignees")
    public ResponseEntity<PostDetailResponse> addCoAssignee(@PathVariable Long postId,
                                                           @Valid @RequestBody AddPostCoAssigneeRequest request,
                                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(postService.addCoAssignee(
                postId, request.managerId(), userDetails.getUserId(), userDetails.getRole()));
    }
}
