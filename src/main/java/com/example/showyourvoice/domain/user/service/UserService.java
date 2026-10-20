package com.example.showyourvoice.domain.user.service;

import com.example.showyourvoice.domain.user.controller.dto.CreateUserRequest;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.domain.User;
import com.example.showyourvoice.domain.user.domain.UserRepository;
import com.example.showyourvoice.domain.user.exception.DuplicateEmailException;
import com.example.showyourvoice.domain.user.exception.UserNotFoundException;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserInfoResponse createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException();
        }

        User user = User.createUser(request.email(), passwordEncoder.encode(request.password()), request.name());
        return UserInfoResponse.from(userRepository.save(user));
    }

    public UserInfoResponse getUserInfo(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return UserInfoResponse.from(user);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }
}
