package com.example.showyourvoice.domain.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.example.showyourvoice.domain.place.domain.AssignmentRuleRepository;
import com.example.showyourvoice.domain.user.domain.Role;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.service.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTests {

    @Mock private UserRepository userRepository;
    @Mock private AssignmentRuleRepository assignmentRuleRepository;
    @InjectMocks private UserService userService;

    @Test
    void returnsEmptyListWhenNoTeachersExist() {
        when(userRepository.findAllByRoleOrderByNameAscIdAsc(Role.TEACHER)).thenReturn(List.of());
        when(assignmentRuleRepository.findAllByOrderByPlaceNameAscPlaceIdAscIdAsc()).thenReturn(List.of());

        assertThat(userService.getManagers()).isEmpty();
    }
}
