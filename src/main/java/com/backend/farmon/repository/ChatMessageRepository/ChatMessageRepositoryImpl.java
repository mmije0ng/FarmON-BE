package com.backend.farmon.repository.ChatMessageRepository;

import com.backend.farmon.domain.ChatMessage;
import com.backend.farmon.domain.QChatMessage;
import com.backend.farmon.domain.enums.ChatMessageType;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class ChatMessageRepositoryImpl implements ChatMessageRepositoryCustom {

    private final JPAQueryFactory queryFactory;
    QChatMessage chatMessage = QChatMessage.chatMessage;

    // 채팅방 아이디와 일치하는 텍스트, 이미지 타입 채팅 메시지 무한스크롤 조회
    @Override
    public Slice<ChatMessage> findTextImageMessagesByChatRoomId(Long chatRoomId, LocalDateTime lastCreatedAt, Long lastMessageId, int pageSize) {
        List<ChatMessage> content = queryFactory
                .selectFrom(chatMessage)
                .where(
                        chatMessage.chatRoom.id.eq(chatRoomId),
                        chatMessage.type.in(ChatMessageType.TEXT, ChatMessageType.IMAGE),
                        cursorCondition(lastCreatedAt, lastMessageId) // 커서 조건
                )
                .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc()) // 최신순 정렬
                .limit(pageSize + 1) // 요청한 개수보다 1개 더 조회
                .fetch();

        boolean hasNext = content.size() > pageSize; // 다음 페이지가 있는지 판단
        if (hasNext) {
            content.remove(pageSize); // 추가 조회된 1개는 제거
        }

        return new SliceImpl<>(content, PageRequest.of(0, pageSize), hasNext);
    }

    private BooleanExpression cursorCondition(LocalDateTime lastCreatedAt, Long lastMessageId) {
        if (lastCreatedAt == null || lastMessageId == null) {
            return null; // 첫 요청이면 커서 조건 없음
        }
        return chatMessage.createdAt.lt(lastCreatedAt)
                .or(chatMessage.createdAt.eq(lastCreatedAt).and(chatMessage.id.lt(lastMessageId)));
    }

//    @Override
//    public Slice<ChatMessage> findTextImageMessagesByChatRoomId(Long chatRoomId, LocalDateTime lastCreatedAt, Long lastMessageId, int pageSize) {
//        List<ChatMessage> content = queryFactory
//                .selectFrom(chatMessage)
//                .where(
//                        chatMessage.chatRoom.id.eq(chatRoomId),
//                        chatMessage.type.in(ChatMessageType.TEXT, ChatMessageType.IMAGE),
//                        (
//                                lastCreatedAt != null && lastMessageId != null
//                        ) ? (
//                                chatMessage.createdAt.gt(lastCreatedAt)
//                                        .or(
//                                                chatMessage.createdAt.eq(lastCreatedAt)
//                                                        .and(chatMessage.id.gt(lastMessageId))
//                                        )
//                        ) : null
//                )
//                .orderBy(chatMessage.createdAt.desc(), chatMessage.id.desc())
//                .limit(pageSize + 1)
//                .fetch();
//
//        boolean hasNext = content.size() > pageSize;
//        if (hasNext) {
//            content.remove(pageSize);
//        }
//
//        return new SliceImpl<>(content, PageRequest.of(0, pageSize), hasNext);
//    }


//    @Override
//    public Slice<ChatMessage> findTextImageMessagesByChatRoomId(Long chatRoomId, Pageable pageable) {
//        // 요청된 페이지 크기보다 1개 더 가져오기
//        List<ChatMessage> content = queryFactory.selectFrom(chatMessage)
//                .where(
//                        chatMessage.chatRoom.id.eq(chatRoomId),
//                        chatMessage.type.in(ChatMessageType.TEXT, ChatMessageType.IMAGE)
//                )
//                .orderBy(chatMessage.createdAt.desc())
//                .offset(pageable.getOffset())
//                .limit(pageable.getPageSize() + 1) // 추가 데이터 1개 로드
//                .fetch();
//
//        // Slice 객체 반환
//        boolean hasNext = content.size() > pageable.getPageSize();
//        if (hasNext) {
//            content.remove(content.size() - 1); // 추가로 가져온 데이터 제거
//        }
//
//        return new SliceImpl<>(content, pageable, hasNext);
//    }

    // 채팅방 아이디와 일치하는 채팅 메시지 중, 상대방이 보낸 메시지를 읽음 처리
    @Transactional
    @Override
    public void updateMessagesToReadByChatRoomId(Long chatRoomId, Long userId) {
        queryFactory.update(chatMessage)
                .set(chatMessage.isRead, true)
                .where(
                        chatMessage.chatRoom.id.eq(chatRoomId),
                        chatMessage.senderId.notIn(userId),
                        chatMessage.isRead.isFalse()
                )
                .execute();
    }
}
