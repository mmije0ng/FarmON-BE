package com.backend.farmon.config.security;

import com.backend.farmon.apiPayload.ApiResponse;
import com.backend.farmon.apiPayload.code.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
@Component
// 인증되지 않은 요청(Unauthorized, 401)을 공통으로 처리하는 핸들러
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        log.error("인증되지 않은 요청: {}", request.getRequestURI());

        // JSON 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // API 응답 객체 생성
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                ErrorStatus._UNAUTHORIZED.getCode(),
                ErrorStatus._UNAUTHORIZED.getMessage(),
                null
        );

        // JSON 응답 반환
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
