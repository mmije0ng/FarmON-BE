package com.backend.farmon.repository.ChatRoomReposiotry;

import com.backend.farmon.domain.*;
import com.backend.farmon.domain.enums.ChatMessageType;
import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.QueryResults;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ChatRoomRepositoryImpl implements ChatRoomRepositoryCustom {
    private final JPAQueryFactory queryFactory;
    QChatRoom chatRoom = QChatRoom.chatRoom;
    QChatMessage chatMessage = QChatMessage.chatMessage;
    QUser farmer = QUser.user;
    QExpert expert = QExpert.expert;

    // userId, 역할, 검색어가 일치하는 채팅방 페이징 조회
    @Override
    public Page<ChatRoom> findChatRoomsByUserIdAndRoleAndSearch(Long userId, String role, String searchName, Pageable pageable) {
        BooleanBuilder builder = buildBaseQuery(userId, role, searchName, false); // `isUnread = false` (모든 메시지)
        return fetchPagedChatRooms(builder, pageable);
    }

    // userId, 역할, 검색어가 일치하는 안 읽은 채팅방 페이징 조회
    @Override
    public Page<ChatRoom> findUnReadChatRoomsByUserIdAndRoleAndSearch(Long userId, String role, String searchName, Pageable pageable) {
        BooleanBuilder builder = buildBaseQuery(userId, role, searchName, true); // `isUnread = true` (읽지 않은 메시지)
        return fetchPagedChatRooms(builder, pageable);
    }

    // 채팅방 페이징 조건 빌더
    private BooleanBuilder buildBaseQuery(Long userId, String role, String searchName, boolean isUnread) {
        BooleanBuilder builder = new BooleanBuilder();

        // 역할(Role) 설정
        if ("FARMER".equalsIgnoreCase(role)) {
            builder.and(chatRoom.farmer.id.eq(userId));
        } else if ("EXPERT".equalsIgnoreCase(role)) {
            builder.and(chatRoom.expert.user.id.eq(userId));
        } else {
            throw new IllegalArgumentException("Invalid role: " + role);
        }

        // 안 읽은 메시지만 조회하는 경우
        if (isUnread) {
            builder.and(chatMessage.isRead.isFalse());
        }

        // 메시지 유형 필터링 (TEXT, IMAGE만 포함)
        builder.and(chatMessage.type.in(ChatMessageType.TEXT, ChatMessageType.IMAGE));

        // 검색어 필터 추가
        if (searchName != null && !searchName.trim().isEmpty()) {
            BooleanBuilder searchCondition = new BooleanBuilder();
            searchCondition.or(chatRoom.farmer.userName.containsIgnoreCase(searchName));
            searchCondition.or(chatRoom.expert.nickName.containsIgnoreCase(searchName));
            searchCondition.or(chatRoom.expert.isNickNameOnly.eq(false).and(chatRoom.expert.user.userName.containsIgnoreCase(searchName)));
            searchCondition.or(chatRoom.estimate.crop.category.containsIgnoreCase(searchName));
            searchCondition.or(chatRoom.estimate.crop.name.containsIgnoreCase(searchName));
            searchCondition.or(chatRoom.estimate.category.containsIgnoreCase(searchName));

            builder.and(searchCondition);
        }

        return builder;
    }

   // userId와 연관된 페이징된 채팅방 목록 조회
    private Page<ChatRoom> fetchPagedChatRooms(BooleanBuilder builder, Pageable pageable) {
        // 데이터 조회 쿼리
        List<ChatRoom> chatRooms = fetchChatRooms(builder, pageable);

        // 개수 조회 쿼리
        long total = countChatRooms(builder);

        // Page 객체 반환
        return new PageImpl<>(chatRooms, pageable, total);
    }

    // userId와 연관된 채팅방 리스트 조회
    private List<ChatRoom> fetchChatRooms(BooleanBuilder builder, Pageable pageable) {
        return queryFactory
                .selectDistinct(chatRoom)  // 중복 제거
                .from(chatRoom)
                .join(chatMessage).on(chatMessage.chatRoom.id.eq(chatRoom.id)).fetchJoin()
                .where(builder)
                .orderBy(chatRoom.createdAt.desc())
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();
    }

    // 총 채팅방 개수
    private long countChatRooms(BooleanBuilder builder) {
        return Optional.ofNullable(
                queryFactory
                        .select(chatRoom.id.countDistinct())  // DISTINCT 적용
                        .from(chatRoom)
                        .join(chatMessage).on(chatMessage.chatRoom.id.eq(chatRoom.id))
                        .where(builder)
                        .fetchOne()
        ).orElse(0L);
    }

    // 채팅방에서 농업인 여부
    @Override
    public Boolean isFarmerInChatRoom(Long userId, Long chatRoomId) {
        return queryFactory
                .selectOne()
                .from(chatRoom)
                .join(chatRoom.farmer, farmer)
                .where(
                        chatRoom.id.eq(chatRoomId),
                        farmer.id.eq(userId)
                )
                .fetchFirst() != null; // 존재 여부 확인
    }

    // 사용자가 로그인한 역할(role)로 채팅방에 속해 있는지 확인
    @Override
    public String checkUserRoleInChatRoom(Long userId, Long chatRoomId, String role) {
        boolean isUserInChatRoom = queryFactory
                .selectOne()
                .from(chatRoom)
                .leftJoin(chatRoom.farmer, farmer)
                .leftJoin(chatRoom.expert, expert)
                .where(chatRoom.id.eq(chatRoomId)
                        .and(farmer.id.eq(userId).or(expert.user.id.eq(userId))))
                .fetchFirst() != null;

        if (!isUserInChatRoom) {
            return "NOT_IN_CHATROOM"; // 채팅방과 연관이 없는 경우
        }

        if ("FARMER".equalsIgnoreCase(role)) {
            boolean isFarmer = queryFactory
                    .selectOne()
                    .from(chatRoom)
                    .join(chatRoom.farmer, farmer)
                    .where(chatRoom.id.eq(chatRoomId).and(farmer.id.eq(userId)))
                    .fetchFirst() != null;

            return isFarmer ? "MATCHES_ROLE" : "WRONG_ROLE"; // 역할이 일치하는지 확인
        }

        if ("EXPERT".equalsIgnoreCase(role)) {
            boolean isExpert = queryFactory
                    .selectOne()
                    .from(chatRoom)
                    .join(chatRoom.expert, expert)
                    .where(chatRoom.id.eq(chatRoomId).and(expert.user.id.eq(userId)))
                    .fetchFirst() != null;

            return isExpert ? "MATCHES_ROLE" : "WRONG_ROLE"; // 역할이 일치하는지 확인
        }

        return "INVALID_ROLE"; // 올바르지 않은 역할 입력 시
    }
}
