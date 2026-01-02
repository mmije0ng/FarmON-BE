package com.backend.farmon.config.security;

import com.backend.farmon.apiPayload.code.status.ErrorStatus;
import com.backend.farmon.apiPayload.exception.handler.AuthorizationHandler;
import com.backend.farmon.apiPayload.exception.handler.UserHandler;
import com.backend.farmon.repository.UserRepository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Slf4j
@RequiredArgsConstructor
@Component
public class UserAuthorizationUtil {

    private final UserRepository userRepository;

    // 현재 로그인한 유저의 userId 반환해주는 메서드
    public Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication or principal is null");
        }
        Long userId = (Long) authentication.getPrincipal();
        return userId;
    }

    // 현재 로그인한 유저의 role 반환해주는 메서드
    public String getCurrentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication or principal is null");
        }
        GrantedAuthority authority = authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No roles found for the authenticated user"));

        return authority.getAuthority();
    }

    // 현재 로그인한 유저의 ID가 입력받은 ID와 일치하는지 확인
    public boolean isCurrentUserIdMatching(Long userId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null ||
                "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AuthorizationHandler(ErrorStatus._UNAUTHORIZED);
        }

        Object principal = authentication.getPrincipal();

        if (!(principal instanceof Long)) {
            throw new AuthorizationHandler(ErrorStatus._UNAUTHORIZED);
        }

        Long currentUserId = (Long) principal;
        return currentUserId.equals(userId);
    }

    // 현재 로그인한 유저의 role이 입력받은 role과 일치하는지 확인
    public boolean isCurrentUserRoleMatching(String role) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AuthenticationCredentialsNotFoundException("Authentication or principal is null");
        }

        GrantedAuthority authority = authentication.getAuthorities().stream()
                .findFirst()
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("No roles found for the authenticated user"));

        String currentUserRole = authority.getAuthority();
        return currentUserRole.equals(role);
    }

    // 현재 로그인한 유저의 ID와 role이 입력받은 값과 일치하는지 확인
    public boolean isCurrentUserMatching(Long userId, String role) {
        return isCurrentUserIdMatching(userId) && isCurrentUserRoleMatching(role);
    }

}
