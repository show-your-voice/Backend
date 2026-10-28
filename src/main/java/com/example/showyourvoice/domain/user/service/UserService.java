package com.example.showyourvoice.domain.user.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.user.controller.dto.ManagerInfoResponse;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
        Map<Long, List<String>> locationsByTeacher = assignmentRuleRepository.findAllEffectiveByTeacherRole(Role.TEACHER)
                .stream()
                .collect(groupingBy(rule -> rule.assignedTeacher().getId(),
                        mapping(rule -> rule.getPlace().getName(), toList())));

        return managers.stream()
                .map(manager -> ManagerInfoResponse.of(manager,
                        locationsByTeacher.getOrDefault(manager.getId(), List.of()).stream().distinct().toList()))
                .toList();
    }
}
