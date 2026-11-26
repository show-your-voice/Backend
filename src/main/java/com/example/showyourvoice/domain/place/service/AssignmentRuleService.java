package com.example.showyourvoice.domain.place.service;

import com.example.showyourvoice.domain.place.controller.dto.AssignmentRuleResponse;
import com.example.showyourvoice.domain.place.controller.dto.CreateAssignmentRuleRequest;
import com.example.showyourvoice.domain.place.controller.dto.UpdateAssignmentRuleRequest;
import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.place.exception.AssignmentRuleNotFoundException;
import com.example.showyourvoice.domain.place.exception.DuplicateAssignmentRuleException;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.room.domain.Room;
import com.example.showyourvoice.domain.room.domain.RoomRepository;
import com.example.showyourvoice.domain.room.exception.RoomNotFoundException;
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
public class AssignmentRuleService {

    private final AssignmentRuleRepository ruleRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;

    public List<AssignmentRuleResponse> getRules() {
        return ruleRepository.findEffectiveRules().stream().map(AssignmentRuleResponse::from).toList();
    }

    @Transactional
    public AssignmentRuleResponse createRule(CreateAssignmentRuleRequest request) {
        Room room = lockRoom(request.roomId());
        if (ruleRepository.findFirstByRoomIdAndCategoryOrderByIdAsc(room.getId(), request.category()).isPresent()) {
            throw new DuplicateAssignmentRuleException();
        }
        AssignmentRule rule = AssignmentRule.create(room, request.category(), findUser(request.teacherId()));
        return AssignmentRuleResponse.from(ruleRepository.save(rule));
    }

    @Transactional
    public AssignmentRuleResponse updateRule(Long roomId, PostCategory category, UpdateAssignmentRuleRequest request) {
        lockRoom(roomId);
        AssignmentRule rule = ruleRepository.findFirstByRoomIdAndCategoryOrderByIdAsc(roomId, category)
                .orElseThrow(AssignmentRuleNotFoundException::new);
        rule.changeTeacher(findUser(request.teacherId()));
        return AssignmentRuleResponse.from(rule);
    }

    @Transactional
    public void deleteRule(Long roomId, PostCategory category) {
        lockRoom(roomId);
        if (ruleRepository.deleteRules(roomId, category) == 0) {
            throw new AssignmentRuleNotFoundException();
        }
    }

    private Room lockRoom(Long roomId) {
        return roomRepository.findByIdForUpdate(roomId).orElseThrow(RoomNotFoundException::new);
    }

    private User findUser(Long userId) {
        return userRepository.findById(userId).orElseThrow(UserNotFoundException::new);
    }
}
