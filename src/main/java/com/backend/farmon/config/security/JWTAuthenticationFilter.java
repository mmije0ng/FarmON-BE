package com.backend.farmon.config.security;

import com.backend.farmon.apiPayload.code.status.ErrorStatus;
import com.backend.farmon.apiPayload.exception.handler.AuthorizationHandler;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

public class JWTAuthenticationFilter extends OncePerRequestFilter {

    @Autowired
    private JWTUtil tokenGenerator;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        try {
            String token = getJWTFromRequest(request);  // token parsing

            if (StringUtils.hasText(token) && tokenGenerator.validateToken(token)) {// 토큰 유효성 검사
                Long userId = tokenGenerator.extractUserId(token);  // userId 추출
                String role = tokenGenerator.extractRole(token);    // role 추출

                // 로그로 추출한 데이터 출력
                logger.info(String.format("Authenticated User ID: %s, Role: %s", userId, role));

                // 인증 정보 생성
                GrantedAuthority authority = new SimpleGrantedAuthority(role);
                AbstractAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                        userId,  // userId를 principal로 설정
                        null,  // 자격증명은 없으므로 null
                        Collections.singletonList(authority)  // 권한 설정
                );

                authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                // SecurityContext에 인증 정보 설정 (이제 이 사용자는 인증된 사용자로 간주됨)
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            }
        } catch (Exception e) {
            logger.error("Invalid JWT token: " + e.getMessage());

            // 토큰이 없을 경우 에러 던짐
            throw new AuthorizationHandler(ErrorStatus._UNAUTHORIZED);
        }
        filterChain.doFilter(request, response);
    }

    private String getJWTFromRequest(HttpServletRequest request) {
        // "Authorization" 헤더에서 "Bearer" 접두어 제거하고 토큰 추출
        String header = request.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            return header.substring(7, header.length());
        }
        return null;
    }
}