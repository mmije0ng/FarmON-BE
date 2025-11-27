package com.backend.farmon.repository.ChatMessageRepository;

import com.backend.farmon.domain.ChatMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

public interface ChatMessageRepositoryCustom {
    // 채팅방 아이디와 일치하는 채팅 메시지 중, 안 읽은 메시지를 읽음 처리
    void updateMessagesToReadByChatRoomId(Long chatRoomId, Long userId);

    // 채팅방 아이디와 일치하는 텍스트, 이미지 타입 채팅 메시지 리스트 조회
    Slice<ChatMessage> findTextImageMessagesByChatRoomId(Long chatRoomId, LocalDateTime lastCreatedAt, Long lastMessageId, int pageSize);
//    Slice<ChatMessage> findTextImageMessagesByChatRoomId(Long chatRoomId, Pageable pageable);
}