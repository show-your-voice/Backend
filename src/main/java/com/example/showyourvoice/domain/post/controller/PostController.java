package com.example.showyourvoice.domain.post.controller;

import com.example.showyourvoice.domain.post.controller.dto.CreatePostRequest;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostSummaryResponse;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.post.service.PostService;
import com.example.showyourvoice.global.security.CustomUserDetails;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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
    public ResponseEntity<PostDetailResponse> createPost(@Valid @RequestBody CreatePostRequest request,
                                                        @AuthenticationPrincipal CustomUserDetails userDetails) {
        PostDetailResponse response = postService.createPost(request, userDetails.getUserId());
        return ResponseEntity.created(URI.create("/api/posts/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<PostSummaryResponse>> getPosts(@RequestParam(required = false) Long placeId,
                                                              @RequestParam(required = false) PostStatus status) {
        return ResponseEntity.ok(postService.getPosts(placeId, status));
    }

    @GetMapping("/{postId}")
    public ResponseEntity<PostDetailResponse> getPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getPost(postId));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId,
                                           @AuthenticationPrincipal CustomUserDetails userDetails) {
        postService.deletePost(postId, userDetails.getUserId(), userDetails.getRole());
        return ResponseEntity.noContent().build();
    }
}
