package com.backend.farmon.service.ChatMessageService;

import com.backend.farmon.converter.ChatConverter;
import com.backend.farmon.domain.ChatMessage;
import com.backend.farmon.dto.chat.ChatRequest;
import com.backend.farmon.dto.chat.ChatResponse;
import com.backend.farmon.repository.ChatMessageRepository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class ChatMessageQueryServiceImpl implements ChatMessageQueryService{
    private final ChatMessageRepository chatMessageRepository;

    private static final Integer PAGE_SIZE=12;

    // 페이지 정렬, 최신순
    private Pageable pageRequest(Integer pageNumber) {
        return PageRequest.of(pageNumber, PAGE_SIZE, Sort.by("createdAt").descending());
    }

    // 채팅 메시지 내역 조회 & 안 읽은 메시지 읽음 처리
    @Transactional
    @Override
    public ChatResponse.ChatMessageListDTO findChatMessageList(Long userId, Long chatRoomId, LocalDateTime lastCreatedAt, Long lastMessageId) {
        // 안 읽은 메시지들을 읽음 처리
        chatMessageRepository.updateMessagesToReadByChatRoomId(chatRoomId, userId);
        log.info("안 읽은 메시지들 읽음 처리 완료 - chatRoomId: {}", chatRoomId);

        // 채팅 메시지 내역 조회 (EXIT, COMPLETE 제외)
//        Slice<ChatMessage> chatMessageList = chatMessageRepository.findTextImageMessagesByChatRoomId(chatRoomId, pageRequest(pageNumber));
        Slice<ChatMessage> chatMessageList = chatMessageRepository.findTextImageMessagesByChatRoomId(chatRoomId, lastCreatedAt, lastMessageId, PAGE_SIZE);
        log.info("채팅 메시지 내역 조회 완료 - chatRoomId: {}", chatRoomId);

        return ChatConverter.toChatMessageListDTO(chatMessageList, userId);
    }
}
