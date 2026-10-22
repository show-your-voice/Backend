package com.example.showyourvoice.domain.post.service;

import com.example.showyourvoice.domain.place.domain.Place;
import com.example.showyourvoice.domain.place.domain.PlaceRepository;
import com.example.showyourvoice.domain.place.domain.PlaceTeacherAssignment;
import com.example.showyourvoice.domain.place.domain.PlaceTeacherAssignmentRepository;
import com.example.showyourvoice.domain.place.exception.PlaceNotFoundException;
import com.example.showyourvoice.domain.post.controller.dto.CreatePostRequest;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.post.exception.PostNotFoundException;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PlaceRepository placeRepository;
    private final PlaceTeacherAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public PostDetailResponse createPost(CreatePostRequest request, Long userId) {
        User reporter = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Place place = placeRepository.findById(request.placeId()).orElseThrow(PlaceNotFoundException::new);
        User teacher = assignmentRepository.findByPlaceId(place.getId())
                .map(PlaceTeacherAssignment::assignedTeacher)
                .orElse(null);

        Post post = Post.builder()
                .title(request.title()).content(request.content()).improvement(request.improvement())
                .category(request.category()).place(place).reporter(reporter).assignedTeacher(teacher).build();
        return PostDetailResponse.from(postRepository.save(post));
    }

    public PostDetailResponse getPost(Long postId) {
        return PostDetailResponse.from(postRepository.findById(postId).orElseThrow(PostNotFoundException::new));
    }
}
