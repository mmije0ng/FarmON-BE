package com.backend.farmon.config.security;

import com.backend.farmon.apiPayload.ApiResponse;
import com.backend.farmon.apiPayload.code.status.ErrorStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDateTime;

@Slf4j
@AllArgsConstructor
@Component
// 서버에 특정 리소스에 접근할 권한(Forbidden, 403)이 없을 때 공통으로 처리하는 핸들러
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {
        log.error("접근 권한이 없는 요청: {}", request.getRequestURI());

        // JSON 응답 설정
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        // API 응답 객체 생성
        ApiResponse<Void> errorResponse = ApiResponse.onFailure(
                ErrorStatus._FORBIDDEN.getCode(),
                ErrorStatus._FORBIDDEN.getMessage(),
                null
        );

        // JSON 응답 반환
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}