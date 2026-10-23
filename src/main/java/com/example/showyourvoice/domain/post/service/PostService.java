package com.example.showyourvoice.domain.post.service;

import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.place.exception.PlaceNotFoundException;
import com.example.showyourvoice.domain.post.controller.dto.CreatePostRequest;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostSummaryResponse;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.post.exception.PostNotFoundException;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PlaceRepository placeRepository;
    private final AssignmentRuleRepository assignmentRuleRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostDetailResponse createPost(CreatePostRequest request, Long userId) {
        User author = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Place place = placeRepository.findById(request.placeId()).orElseThrow(PlaceNotFoundException::new);
        User assignee = assignmentRuleRepository.findFirstByPlaceIdAndCategoryOrderByIdAsc(place.getId(), request.category())
                .map(AssignmentRule::assignedTeacher)
                .orElse(null);

        Post post = Post.builder()
                .title(request.title()).content(request.content()).improvement(request.improvement())
                .category(request.category()).place(place).posX(request.posX()).posY(request.posY())
                .author(author).assignee(assignee).build();
        return PostDetailResponse.from(postRepository.save(post));
    }

    public List<PostSummaryResponse> getPosts(Long placeId, PostStatus status) {
        return postRepository.findAllByFilter(placeId, status).stream()
                .map(PostSummaryResponse::from)
                .toList();
    }

    public PostDetailResponse getPost(Long postId) {
        return PostDetailResponse.from(postRepository.findById(postId).orElseThrow(PostNotFoundException::new));
    }
}
