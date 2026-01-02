package com.backend.farmon.config.chat;
import com.backend.farmon.service.ChatRoomService.ChatRoomCommandService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import java.util.concurrent.ConcurrentHashMap;
@Slf4j
@RequiredArgsConstructor
@Component
public class WebSocketSessionManager {
    private final ConcurrentHashMap<String, SessionInfo> sessionToUserMap = new ConcurrentHashMap<>();
    private final ChatRoomCommandService chatRoomCommandService;

    // 연결 시 세션과 사용자 정보 매핑
    public void storeUserSession(String sessionId, String token, String role, Long userId, Long chatRoomId, Boolean enterStatus) {
        SessionInfo sessionInfo = new SessionInfo(token, role, userId, chatRoomId, enterStatus);
        sessionToUserMap.put(sessionId, sessionInfo);
    }

    // 연결 해제 시 세션 정보 사용
    public void handleSessionDisconnect(Message<?> message) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(message);
        String sessionId = headerAccessor.getSessionId();
        SessionInfo sessionInfo = sessionToUserMap.get(sessionId);

        if (sessionInfo != null) {
            System.out.println("Disconnected session info: " + sessionInfo);
            String role = sessionInfo.getRole();
            Long userId = sessionInfo.getUserId();
            Long chatRoomId = sessionInfo.getChatRoomId();
            boolean isExpert = sessionInfo.getRole().equals("EXPERT");

            // 채팅방 입장 시간 변경
            sessionToUserMap.remove(sessionId); // 세션 제거
            log.info("세션 제거 성공");
            log.info("stomp 연결 해제된 사용자 정보 - role: {}, userId: {}, chatRoomId: {}", role, userId, chatRoomId);

        } else {
            System.out.println("No session info found for session: " + sessionId);
        }
    }
}