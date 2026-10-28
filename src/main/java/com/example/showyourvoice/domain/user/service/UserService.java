package com.example.showyourvoice.domain.user.service;

import com.example.showyourvoice.domain.place.domain.AssignmentRule;
import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.post.domain.PostCategory;
import com.example.showyourvoice.domain.user.controller.dto.ManagerInfoResponse;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final AssignmentRuleRepository assignmentRuleRepository;

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return UserInfoResponse.from(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<ManagerInfoResponse> getManagers() {
        List<User> managers = userRepository.findAllByRoleOrderByNameAscIdAsc(Role.TEACHER);
        Map<Long, Set<String>> locationsByTeacher = getLocationsByTeacher();

        return managers.stream()
                .map(manager -> ManagerInfoResponse.of(manager,
                        List.copyOf(locationsByTeacher.getOrDefault(manager.getId(), Set.of()))))
                .toList();
    }

    private Map<Long, Set<String>> getLocationsByTeacher() {
        Set<PlaceCategory> processedPlaceCategories = new HashSet<>();
        Map<Long, Set<String>> locationsByTeacher = new HashMap<>();

        for (AssignmentRule rule : assignmentRuleRepository.findAllByOrderByPlaceNameAscPlaceIdAscIdAsc()) {
            PlaceCategory placeCategory = new PlaceCategory(rule.getPlace().getId(), rule.getCategory());
            if (!processedPlaceCategories.add(placeCategory)) {
                continue;
            }

            User teacher = rule.getTeacher();
            if (teacher.getRole() != Role.TEACHER) {
                continue;
            }

            locationsByTeacher.computeIfAbsent(teacher.getId(), id -> new LinkedHashSet<>())
                    .add(rule.getPlace().getName());
        }
        return locationsByTeacher;
    }

    private record PlaceCategory(Long placeId, PostCategory category) {
    }
}
