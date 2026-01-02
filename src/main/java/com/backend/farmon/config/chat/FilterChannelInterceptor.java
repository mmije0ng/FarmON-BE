package com.backend.farmon.config.chat;

import com.backend.farmon.config.security.JWTUtil;
import com.backend.farmon.service.ChatRoomService.ChatRoomCommandService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 99) // 우선순위 올리기
public class FilterChannelInterceptor implements ChannelInterceptor {

    private final WebSocketSessionManager webSocketSessionManager;
    private final JWTUtil jwtUtil;
    private final ChatRoomCommandService chatRoomCommandService;
    private static final String BEARER_PREFIX = "Bearer ";

    // 메시지가 채널로 전송되기 전 호출되는 메서드
    // 메시지 전송 전에 인증 토큰을 검증하거나, 메시지 내용을 필터링 가능
    @Override
    @Transactional
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        StompCommand command = headerAccessor.getCommand();

        if (!"HEARTBEAT".equals(headerAccessor.getMessageType().name())) {
            log.info("full message:" + message);
        }

        if (command == null) {
            return message;
        }

        switch (command) {
            case CONNECT: // 연결
                return handleStompConnect(message, headerAccessor);
            case DISCONNECT: // 연결 해제
                return handleStompDisconnect(message, headerAccessor);
            default:
                return message;
        }
    }

    // stomp 최초 연결 시
    private Message<?> handleStompConnect(Message<?> message, StompHeaderAccessor headerAccessor) {
        log.info("stomp 연결 성공");

        try {
            // 채팅방 입장 시간 변경, 세션에 저장할 정보 추출
            String token = getToken(headerAccessor);
            String role = jwtUtil.extractRole(getToken(headerAccessor));
            Long userId = jwtUtil.extractUserId(token);
            log.info("stomp 연결된 사용자 정보 - role: {}, userId: {}", role, userId);

            return message; // 메시지 전달
        } catch (Exception e) {
            log.error("CONNECT 처리 중 오류 발생: " + e.getMessage());
            return buildErrorMessage(headerAccessor.getSessionId(), e.getMessage());
        }
    }

    // stomp 연결 해제 시
    private Message<?> handleStompDisconnect(Message<?> message, StompHeaderAccessor headerAccessor) {
        try {
            log.info("stomp 연결 해제 성공");

            return message; // 메시지 전달
        } catch (Exception e) {
            log.error("DISCONNECT 처리 중 오류 발생: " + e.getMessage());
            return buildErrorMessage(headerAccessor.getSessionId(), e.getMessage());
        }
    }

    // 연결 헤더에서 토큰 추출
    private String getToken(StompHeaderAccessor headerAccessor) {
        List<String> authorizationHeaders = headerAccessor.getNativeHeader("Authorization");
        if (authorizationHeaders == null || authorizationHeaders.isEmpty()) {
            throw new IllegalArgumentException("stomp 연결 헤더에 Authorization 토큰 없음");
        }

        String authorizationHeader = authorizationHeaders.get(0);
        if (!authorizationHeader.startsWith(BEARER_PREFIX)) {
            throw new IllegalArgumentException("Authorization 헤더는 Bearer로 시작해야 합니다.");
        }

        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private Message<?> buildErrorMessage(String sessionId, String errorMessage) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(StompCommand.ERROR);
        headerAccessor.setSessionId(sessionId);
        headerAccessor.setMessage(errorMessage);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("isSuccess", false);
        responseBody.put("message", "채팅 연결 관련 에러: " + errorMessage);
        responseBody.put("code", "CHAT_MESSAGE_4001");

        String jsonResponse = convertToJson(responseBody);

        return MessageBuilder.withPayload(jsonResponse)
                .setHeaders(headerAccessor)
                .build();
    }

    private String convertToJson(Map<String, Object> responseBody) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(responseBody);
        } catch (JsonProcessingException e) {
            log.error("JSON 변환 중 오류 발생: " + e.getMessage());
            return "{\"isSuccess\":false,\"message\":\"JSON 처리 오류\",\"timestamp\":\"" + Instant.now().toString() + "\"}";
        }
    }
}
