package com.backend.farmon.dto.chat;

import com.backend.farmon.validaton.annotation.EqualsUserId;
import com.backend.farmon.validaton.annotation.ExistUser;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

public class ChatRequest {

    @ToString
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(description = "전송 또는 수신할 채팅 메시지 정보")
    public static class ChatMessageDTO {

        @ExistUser
        @Schema(description = "보낸 사람 아이디, 현재 로그인한 사용자의 userId와 동일", example = "1")
        Long senderId;

        @Schema(description = "메시지 내용, 이미지일 경우 클라이언트에서 이미지 파일을 Base64로 인코딩하여 전송 필요", example = "안녕하세요. 견적 신청하셨나요?")
        String messageContent;

        @Schema(description = "메시지 타입, " +
                "텍스트 메시지 전송이라면 TEXT, 이미지 전송이라면 IMAGE, 컨설팅 완료라면 COMPLETE, 채팅방 퇴장이라면 EXIT", example = "TEXT")
        String messageType;

        @Schema(description = "보낸 시간", example = "오후 5:22")
        String sendTime;

        @Schema(description = "내가 보낸 메시지인지 여부, 내가 보낸 메시지라면 true, 받은 메시지라면 false", example = "true")
        Boolean isMine;

        @Schema(description = "견적 완료 여부, 농업인 전문가 모두 컨설팅 완료 시 true", example = "true")
        Boolean isEstimateComplete;
    }

    @ToString
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestDTO {
        LocalDateTime lastCreatedAt;
        Long lastMessageId;
    }


}
