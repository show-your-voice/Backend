package com.example.showyourvoice.domain.auth.service;

import com.example.showyourvoice.domain.auth.controller.dto.LoginRequest;
import com.example.showyourvoice.domain.auth.exception.LoginFailedException;
import com.example.showyourvoice.domain.user.controller.dto.UserInfoResponse;
import com.example.showyourvoice.domain.user.service.UserService;
import com.example.showyourvoice.global.security.CustomUserDetails;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final UserService userService;
    private final SecurityContextLogoutHandler logoutHandler = new SecurityContextLogoutHandler();

    public UserInfoResponse login(LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        Authentication authentication = authenticate(request);

        // 세션 고정 공격 방지: 로그인 전 세션이 있으면 ID를 새로 발급한다.
        if (httpRequest.getSession(false) != null) {
            httpRequest.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, httpRequest, httpResponse);

        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        return userService.getUserInfo(userDetails.getUserId());
    }

    public void logout(HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        logoutHandler.logout(httpRequest, httpResponse, SecurityContextHolder.getContext().getAuthentication());
    }

    private Authentication authenticate(LoginRequest request) {
        try {
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(request.email(), request.password()));
        } catch (AuthenticationException e) {
            throw new LoginFailedException();
        }
    }
}
