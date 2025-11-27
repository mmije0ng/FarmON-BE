package com.backend.farmon.controller;

import com.backend.farmon.apiPayload.ApiResponse;
import com.backend.farmon.dto.chat.ChatRequest;
import com.backend.farmon.dto.chat.ChatResponse;
import com.backend.farmon.service.ChatMessageService.ChatMessageQueryService;
import com.backend.farmon.service.ChatRoomService.ChatRoomCommandService;
import com.backend.farmon.service.ChatRoomService.ChatRoomQueryService;
import com.backend.farmon.validaton.annotation.CheckPage;
import com.backend.farmon.validaton.annotation.EqualsUserId;
import com.backend.farmon.validaton.annotation.ExistChatRoom;
import com.backend.farmon.validaton.annotation.ExistUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;


@Tag(name = "채팅 페이지", description = "채팅에 관한 API")
@Slf4j
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/chat")
public class ChatRoomController {

    private final ChatRoomCommandService chatRoomCommandService;
    private final ChatRoomQueryService chatRoomQueryService;
    private final ChatMessageQueryService chatMessageQueryService;

    // 전체 채팅 목록 조회
    @Operation(
            summary = "사용자 역할 & 검색어에 따른 전체 채팅 목록 조회 API",
            description = "사용자 역할과 검색어에 따른 전체 채팅 목록을 조회하는 API이며, 페이징을 포함합니다. " +
                    "로그인 한 사용자의 역할이 농업인일 경우 농업인으로 대화한 채팅 목록을, 전문가일 경우 전문가로 대화한 채팅 목록을 반환합니다. " +
                    "채팅 상대 이름 혹은 작물 이름으로 검색하는 경우에는 검색어를 쿼리 스트링에 입력해 주세요. " +
                    "검색어를 입력하지 않은 경우에는 전체 채팅 목록을 조회합니다. " +
                    "유저 아이디, 읽음 여부 필터, 검색어, 페이지 번호를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200",description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "PAGE4001", description = "페이지 번호는 1 이상이어야 합니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "read", description = "읽음 여부 필터링. 안 읽은 채팅방만 필터링 시에는 1, 이외에는 0 입니다.", example = "0", required = true),
            @Parameter(name = "page", description = "페이지 번호, 1부터 시작입니다.", example = "1", required = true),
            @Parameter(name = "searchName", description = "검색어", example = "병해중전문가"),
    })
    @GetMapping("/rooms/all")
    public ApiResponse<ChatResponse.ChatRoomListDTO> getChatRoomPage (@RequestParam(name = "userId") @EqualsUserId @ExistUser Long userId,
                                                                      @RequestParam(name = "read") Integer read,
                                                                      @CheckPage Integer page,
                                                                      @RequestParam(name = "searchName", required = false) String searchName){

        ChatResponse.ChatRoomListDTO response = chatRoomQueryService.findChatRoomByRoleAndSearch(userId, read, searchName, page);

        return ApiResponse.onSuccess(response);
    }


    // 전문가가 농업인의 견적을 보고 채팅 신청 (채팅방 생성)
    @Operation(
            summary = "전문가가 농업인의 견적을 보고 채팅을 신청하여 채팅방을 생성",
            description = "전문가가 농업인의 견적을 보고 채팅을 신청하여 채팅방을 생성하는 API 입니다. " +
                    "유저 아이디, 견적 아이디를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200",description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "EXPERT4001", description = "아이디와 일치하는 전문가가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4031", description = "채팅방 생성은 전문가만 가능합니다", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "ESTIMATE4001", description = "견적 아이디와 일치하는 견적이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "estimateId", description = "채팅하려는 견적의 아이디", example = "1", required = true),
    })
    @PostMapping("/room")
    public ApiResponse<ChatResponse.ChatRoomCreateDTO> postChatRoom (@RequestParam(name = "userId") @EqualsUserId Long userId,
                                                                     @RequestParam(name = "estimateId") Long estimateId) {

        ChatResponse.ChatRoomCreateDTO response = chatRoomCommandService.addChatRoom(userId, estimateId);

        return ApiResponse.onSuccess(response);
    }


    // 채팅 대화 상대의 정보 조회
    @Operation(
            summary = " 채팅방의 정보 조회",
            description = "채팅방 아이디와 일치하는 채팅방 입장에 입장하여 채팅 대화 상대의 정보와 컨설팅 완료 정보를 가져오는 API 입니다. " +
                    "유저 아이디, 채팅방 아이디를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200",description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4001", description = "채팅방 아이디와 일치하는 채팅방이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4032", description = "해당 채팅방에 농업인 또는 전문가로 속하지 않는 사용자 입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4033", description = "로그인한 역할과 채팅방에서의 역할이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "chatRoomId", description = "조회하려는 채팅방의 아이디", example = "1", required = true),
    })
    @GetMapping("/room")
    public ApiResponse<ChatResponse.ChatRoomDataDTO> getChatRoomData (@RequestParam(name = "userId") @EqualsUserId @ExistUser Long userId,
                                                                          @RequestParam(name = "chatRoomId") Long chatRoomId) {
        ChatResponse.ChatRoomDataDTO response = chatRoomQueryService.findChatRoomDataAndChangeUnreadMessage(userId, chatRoomId);

        return ApiResponse.onSuccess(response);
    }

    // 채팅 메시지 내역 조회
    @Operation(
            summary = "채팅방의 대화 내역 조회",
            description = "채팅방 아이디와 일치하는 채팅방의 채팅 대화 내역을 무한스크롤로 받는 API 입니다. " +
                    "유저 아이디, 채팅방 아이디, 페이지 번호를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200",description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4001", description = "채팅방 아이디와 일치하는 채팅방이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4032", description = "해당 채팅방에 농업인 또는 전문가로 속하지 않는 사용자 입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4033", description = "로그인한 역할과 채팅방에서의 역할이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "PAGE4001", description = "페이지 번호는 1 이상이어야 합니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "chatRoomId", description = "대화 내역(메시지)을 조회하려는 채팅방의 아이디", example = "1", required = true),
//            @Parameter(name = "page", description = "페이지 번호, 1부터 시작입니다.", example = "1", required = true)
    })
    @GetMapping("/room/message")
    public ApiResponse<ChatResponse.ChatMessageListDTO> getChatMessageList (@RequestParam(name = "userId") @EqualsUserId @ExistUser Long userId,
                                                                            @RequestParam(name = "chatRoomId") @ExistChatRoom Long chatRoomId,
//                                                                            @CheckPage Integer page,
                                                                            @RequestParam(name = "lastCreatedAt") LocalDateTime lastCreatedAt,
                                                                            @RequestParam(name = "lastMessageId") Long lastMessageId) {
        ChatResponse.ChatMessageListDTO response = chatMessageQueryService.findChatMessageList(userId, chatRoomId, lastCreatedAt, lastMessageId);
//        ChatResponse.ChatMessageListDTO response = chatMessageQueryService.findChatMessageList(userId, chatRoomId, page);

        return ApiResponse.onSuccess(response);
    }


    // 채팅방 삭제
    @Operation(
            summary = "채팅방 아이디이와 일치하는 채팅방 삭제",
            description = "채팅방 아이디와 일치하는 채팅방을 삭제하는 API 입니다. " +
                    "유저 아이디, 채팅방 아이디를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4032", description = "해당 채팅방에 농업인 또는 전문가로 속하지 않는 사용자 입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4033", description = "로그인한 역할과 채팅방에서의 역할이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4001", description = "채팅방 아이디와 일치하는 채팅방이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "chatRoomId", description = "삭제하려는 채팅방의 아이디", example = "1", required = true),
    })
    @DeleteMapping("/room")
    public ApiResponse<ChatResponse.ChatRoomDeleteDTO> deleteChatRoom (@RequestParam(name = "userId") @EqualsUserId @ExistUser Long userId,
                                                                         @RequestParam(name = "chatRoomId") Long chatRoomId) {
        ChatResponse.ChatRoomDeleteDTO response = chatRoomCommandService.removeChatRoom(userId, chatRoomId);

        return ApiResponse.onSuccess(response);
    }


    // 채팅방 - 견적 보기
    @Operation(
            summary = "채팅방에서 견적서 상세 보기",
            description = "채팅방에서 현재 대화중인 견적에 대한 상세를 조회하는 API입니다. " +
                    "유저 아이디, 채팅방 아이디를 쿼리 스트링으로 입력해주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4001", description = "채팅방 아이디와 일치하는 채팅방이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4032", description = "해당 채팅방에 농업인 또는 전문가로 속하지 않는 사용자 입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4033", description = "로그인한 역할과 채팅방에서의 역할이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "chatRoomId", description = "채팅방의 아이디", example = "1", required = true)
    })
    @GetMapping("/room/estimate")
    public ApiResponse<ChatResponse.ChatRoomEstimateDTO> getChatRoomEstimate (@RequestParam(name = "userId") @EqualsUserId @ExistChatRoom Long userId,
                                                                              @RequestParam(name = "chatRoomId") Long chatRoomId) {
        ChatResponse.ChatRoomEstimateDTO response = chatRoomQueryService.findChatRoomEstimate(userId, chatRoomId);

        return ApiResponse.onSuccess(response);
    }

    // 채팅에 전송할 이미지 업로드
    @Operation(
            summary = "채팅에 전송할 이미지 업로드",
            description = "채팅방에서 이미지 전송 시, 전송할 이미지 파일을 업로드 후, 해당 이미지 파일의 URL을 응답으로 받는 API 입니다. " +
                    "반환 받은 URL을 채팅방에서 메시지 전송 시 사용하시면 됩니다. " +
                    "유저 아이디, 채팅방 아이디를 쿼리 스트링으로 입력해주세요. " +
                    "이미지 파일을 multipart/form-data로 Request Body에 포함시켜 주세요."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "COMMON200", description = "OK, 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "USER4001", description = "아이디와 일치하는 사용자가 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4001", description = "채팅방 아이디와 일치하는 채팅방이 없습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "AUTHORIZATION4031", description = "인증된 사용자 정보와 요청된 리소스의 사용자 정보가 다릅니다. (userId 불일치)", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4032", description = "해당 채팅방에 농업인 또는 전문가로 속하지 않는 사용자 입니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "CHATROOM4033", description = "로그인한 역할과 채팅방에서의 역할이 일치하지 않습니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "ERROR_UPLOAD_CHAT_IMG", description = "채팅용 이미지 업로드에 실패하였습니다. 관리자에게 문의 바랍니다.", content = @Content(schema = @Schema(implementation = ApiResponse.class))),
    })
    @Parameters({
            @Parameter(name = "userId", description = "로그인한 유저의 아이디(pk)", example = "1", required = true),
            @Parameter(name = "chatRoomId", description = "채팅방의 아이디", example = "1", required = true),

    })
    @PostMapping(value = "/image", consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    public ApiResponse<ChatResponse.ChatImageDTO> postChatImage (@Parameter(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
                                                                     @RequestParam(name = "userId") @EqualsUserId @ExistUser Long userId,
                                                                 @RequestParam(name = "chatRoomId") @ExistChatRoom Long chatRoomId,
                                                                 @RequestPart("chatImage") MultipartFile imageFile) {
        try{
            ChatResponse.ChatImageDTO response = chatRoomQueryService.uploadChatImage(userId, chatRoomId, imageFile);
            return ApiResponse.onSuccess(response);
        } catch (Exception e){
            log.error(e.getMessage());
            return ApiResponse.onFailure("ERROR_UPLOAD_CHAT_IMG","채팅용 이미지 업로드에 실패하였습니다. 관리자에게 문의 바랍니다.",null);
        }
    }
}
