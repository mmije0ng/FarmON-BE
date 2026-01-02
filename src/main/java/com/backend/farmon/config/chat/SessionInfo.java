package com.backend.farmon.config.chat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfo { // stomp 연결 시 저장할 세션 정보
    private String token;
    private String role;
    private Long userId;
    private Long chatRoomId;
    private Boolean enterStatus; // 채팅방 입장 여부
}