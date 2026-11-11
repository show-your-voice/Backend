package com.example.showyourvoice.domain.post.service;

import com.example.showyourvoice.domain.image.domain.ImagePurpose;
import com.example.showyourvoice.domain.image.service.ImageService;
import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.post.controller.dto.CreatePostRequest;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostSummaryResponse;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostCoAssignee;
import com.example.showyourvoice.domain.post.domain.PostCoAssigneeRepository;
import com.example.showyourvoice.domain.post.domain.PostImage;
import com.example.showyourvoice.domain.post.domain.PostImageRepository;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.post.exception.DuplicatePostCoAssigneeException;
import com.example.showyourvoice.domain.post.exception.PostCoAssigneeDeniedException;
import com.example.showyourvoice.domain.post.exception.PostDeleteDeniedException;
import com.example.showyourvoice.domain.post.exception.PostNotFoundException;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import com.example.showyourvoice.domain.room.exception.RoomNotFoundException;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostImageRepository postImageRepository;
    private final PostCoAssigneeRepository postCoAssigneeRepository;
    private final RoomRepository roomRepository;
    private final AssignmentRuleRepository assignmentRuleRepository;
    private final UserRepository userRepository;
    private final ImageService imageService;

    @Transactional
    public PostDetailResponse createPost(CreatePostRequest request, Long userId) {
        User author = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Room room = roomRepository.findById(request.roomId()).orElseThrow(RoomNotFoundException::new);
        User assignee = assignmentRuleRepository.findFirstByRoomIdAndCategoryOrderByIdAsc(room.getId(), request.category())
                .map(AssignmentRule::assignedTeacher)
                .orElse(null);
        List<String> imageUrls = request.imageUrlsOrEmpty();
        imageUrls.forEach(imageUrl -> imageService.verifyUploaded(imageUrl, ImagePurpose.POST));

        Post post = postRepository.save(Post.builder()
                .title(request.title()).content(request.content()).improvement(request.improvement())
                .category(request.category()).room(room).posX(request.posX()).posY(request.posY())
                .author(author).assignee(assignee).build());
        List<PostImage> images = postImageRepository.saveAll(IntStream.range(0, imageUrls.size())
                .mapToObj(i -> PostImage.builder().post(post).imageUrl(imageUrls.get(i)).sortOrder(i).build())
                .toList());
        return PostDetailResponse.of(post, images);
    }

    public List<PostSummaryResponse> getPosts(Long placeId, PostStatus status) {
        return postRepository.findAllByFilter(placeId, status).stream()
                .map(PostSummaryResponse::from)
                .toList();
    }

    public PostDetailResponse getPost(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        return detailResponse(post);
    }

    @Transactional
    public PostDetailResponse addCoAssignee(Long postId, Long managerId, Role role) {
        if (role != Role.TEACHER) {
            throw new PostCoAssigneeDeniedException();
        }

        Post post = postRepository.findByIdForUpdate(postId).orElseThrow(PostNotFoundException::new);
        User manager = userRepository.findById(managerId).orElseThrow(UserNotFoundException::new);
        PostCoAssignee assignment = PostCoAssignee.assign(post, manager);
        if ((post.getAssignee() != null && post.getAssignee().getId().equals(managerId))
                || postCoAssigneeRepository.existsByPostIdAndManagerId(postId, managerId)) {
            throw new DuplicatePostCoAssigneeException();
        }

        postCoAssigneeRepository.save(assignment);
        return detailResponse(post);
    }

    private PostDetailResponse detailResponse(Post post) {
        List<UserInfoResponse> coAssignees = postCoAssigneeRepository.findAllByPostIdOrderByIdAsc(post.getId()).stream()
                .map(assignment -> UserInfoResponse.from(assignment.getManager()))
                .toList();
        return PostDetailResponse.of(post, postImageRepository.findAllByPostIdOrderBySortOrderAsc(post.getId()), coAssignees);
    }

    @Transactional
    public void deletePost(Long postId, Long userId, Role role) {
        Post post = postRepository.findByIdForUpdate(postId).orElseThrow(PostNotFoundException::new);
        if (!post.isDeletableBy(userId, role)) {
            throw new PostDeleteDeniedException();
        }

        postImageRepository.deleteAllByPostId(postId);
        postCoAssigneeRepository.deleteAllByPostId(postId);
        postRepository.delete(post);
    }
}
