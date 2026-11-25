package com.example.showyourvoice.domain.post.service;

import com.example.showyourvoice.domain.image.domain.ImagePurpose;
import com.example.showyourvoice.domain.image.service.ImageService;
import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.post.controller.dto.PostDetailResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostPageResponse;
import com.example.showyourvoice.domain.post.controller.dto.PostSummaryResponse;
import com.example.showyourvoice.domain.post.controller.dto.SavePostRequest;
import com.example.showyourvoice.domain.post.domain.Post;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.post.domain.PostCoAssignee;
import com.example.showyourvoice.domain.post.domain.PostCoAssigneeRepository;
import com.example.showyourvoice.domain.post.domain.PostCreatedEvent;
import com.example.showyourvoice.domain.post.domain.PostImage;
import com.example.showyourvoice.domain.post.domain.PostImageRepository;
import com.example.showyourvoice.domain.post.domain.PostRepository;
import com.example.showyourvoice.domain.post.domain.PostStatus;
import com.example.showyourvoice.domain.post.exception.DuplicatePostCoAssigneeException;
import com.example.showyourvoice.domain.post.exception.PostCoAssigneeDeniedException;
import com.example.showyourvoice.domain.post.exception.PostDeleteDeniedException;
import com.example.showyourvoice.domain.post.exception.PostEditDeniedException;
import com.example.showyourvoice.domain.post.exception.PostNotFoundException;
import com.example.showyourvoice.domain.post.exception.PostStatusChangeDeniedException;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import com.example.showyourvoice.domain.room.exception.RoomNotFoundException;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.IntStream;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public PostDetailResponse createPost(SavePostRequest request, Long userId) {
        User author = userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
        Room room = roomRepository.findById(request.roomId()).orElseThrow(RoomNotFoundException::new);
        User assignee = findAssignee(room.getId(), request.category());
        List<String> imageUrls = request.imageUrlsOrEmpty();
        imageUrls.forEach(imageUrl -> imageService.verifyUploaded(imageUrl, ImagePurpose.POST));

        Post post = postRepository.save(Post.builder()
                .title(request.title()).content(request.content()).improvement(request.improvement())
                .category(request.category()).room(room).posX(request.posX()).posY(request.posY())
                .author(author).assignee(assignee).build());
        List<PostImage> images = saveImages(post, imageUrls);
        eventPublisher.publishEvent(PostCreatedEvent.from(post));
        return PostDetailResponse.of(post, images, List.of(), userId, author.getRole() == Role.TEACHER);
    }

    @Transactional
    public PostDetailResponse updatePost(Long postId, SavePostRequest request, Long userId, Role role) {
        Post post = postRepository.findByIdForUpdate(postId).orElseThrow(PostNotFoundException::new);
        if (!post.isEditableBy(userId)) {
            throw new PostEditDeniedException();
        }

        Room room = roomRepository.findById(request.roomId()).orElseThrow(RoomNotFoundException::new);
        User assignee = post.getAssignee();
        if (!post.getRoom().getId().equals(room.getId()) || post.getCategory() != request.category()) {
            assignee = findAssignee(room.getId(), request.category());
            if (assignee != null) {
                postCoAssigneeRepository.deleteByPostIdAndManagerId(postId, assignee.getId());
            }
        }
        post.revise(request.title(), request.content(), request.improvement(), request.category(),
                room, request.posX(), request.posY(), assignee);
        List<PostImage> images = replaceImages(post, request.imageUrlsOrEmpty());
        return PostDetailResponse.of(post, images, findCoAssignees(postId), userId, role == Role.TEACHER);
    }

    private User findAssignee(Long roomId, PostCategory category) {
        return assignmentRuleRepository.findFirstByRoomIdAndCategoryOrderByIdAsc(roomId, category)
                .map(AssignmentRule::assignedTeacher)
                .orElse(null);
    }

    private List<PostImage> replaceImages(Post post, List<String> imageUrls) {
        List<PostImage> existing = postImageRepository.findAllByPostIdOrderBySortOrderAsc(post.getId());
        List<String> existingUrls = existing.stream().map(PostImage::getImageUrl).toList();
        if (existingUrls.equals(imageUrls)) {
            return existing;
        }

        imageUrls.stream().filter(url -> !existingUrls.contains(url))
                .forEach(url -> imageService.verifyUploaded(url, ImagePurpose.POST));
        postImageRepository.deleteAll(existing);
        return saveImages(post, imageUrls);
    }

    private List<PostImage> saveImages(Post post, List<String> imageUrls) {
        return postImageRepository.saveAll(IntStream.range(0, imageUrls.size())
                .mapToObj(i -> PostImage.builder().post(post).imageUrl(imageUrls.get(i)).sortOrder(i).build())
                .toList());
    }

    public PostPageResponse getPosts(Long placeId, PostStatus status, Pageable pageable) {
        // 정렬은 최신순으로 고정하고, 요청의 sort 값은 쓰지 않는다.
        Pageable page = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        return PostPageResponse.from(postRepository.findAllByFilter(placeId, status, page));
    }

    public List<PostSummaryResponse> getAssignedPosts(Long teacherId, PostStatus status) {
        return postRepository.findAssignedTo(teacherId, status).stream()
                .map(PostSummaryResponse::from)
                .toList();
    }

    public PostDetailResponse getPost(Long postId, Long userId, Role role) {
        Post post = postRepository.findById(postId).orElseThrow(PostNotFoundException::new);
        return detailResponse(post, userId, role);
    }

    @Transactional
    public PostDetailResponse changeStatus(Long postId, PostStatus status, Long userId, Role role) {
        if (role != Role.TEACHER) {
            throw new PostStatusChangeDeniedException();
        }
        Post post = postRepository.findByIdForUpdate(postId).orElseThrow(PostNotFoundException::new);
        List<UserInfoResponse> coAssignees = findCoAssignees(postId);
        if (!canChangeStatus(post, userId, role, coAssignees)) {
            throw new PostStatusChangeDeniedException();
        }

        post.changeStatus(status, LocalDateTime.now().truncatedTo(ChronoUnit.MICROS));
        return detailResponse(post, userId, coAssignees, true);
    }

    @Transactional
    public PostDetailResponse addCoAssignee(Long postId, Long managerId, Long userId, Role role) {
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
        return detailResponse(post, userId, role);
    }

    private PostDetailResponse detailResponse(Post post, Long userId, Role role) {
        List<UserInfoResponse> coAssignees = findCoAssignees(post.getId());
        return detailResponse(post, userId, coAssignees, canChangeStatus(post, userId, role, coAssignees));
    }

    private PostDetailResponse detailResponse(Post post, Long userId, List<UserInfoResponse> coAssignees,
                                              boolean canChangeStatus) {
        return PostDetailResponse.of(post, postImageRepository.findAllByPostIdOrderBySortOrderAsc(post.getId()),
                coAssignees, userId, canChangeStatus);
    }

    private List<UserInfoResponse> findCoAssignees(Long postId) {
        return postCoAssigneeRepository.findAllByPostIdOrderByIdAsc(postId).stream()
                .map(assignment -> UserInfoResponse.from(assignment.getManager()))
                .toList();
    }

    private boolean canChangeStatus(Post post, Long userId, Role role, List<UserInfoResponse> coAssignees) {
        return role == Role.TEACHER && (post.getAuthor().getId().equals(userId)
                || (post.getAssignee() != null && post.getAssignee().getId().equals(userId))
                || coAssignees.stream().anyMatch(assignee -> assignee.id().equals(userId)));
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
