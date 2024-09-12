package server.cubeTalk.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import server.cubeTalk.chat.model.dto.*;
import server.cubeTalk.chat.service.ChatRoomService;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Tag(name = "채팅", description = "채팅 API")
@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    private final ChatRoomService chatRoomService;

    @PostMapping()
    @Operation(summary = "채팅방 생성 API", description = "채팅방 생성시 사용하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomCreateResponseDto>> createChatRoom(
           @Valid @RequestBody ChatRoomCreateRequestDto requestDto) {

            ChatRoomCreateResponseDto responseDto = chatRoomService.createChatRoom(requestDto);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.CREATED);
    }

    @PostMapping("/{id}/participants")
    @Operation(summary = "채팅방 참가 API", description = "채팅방 참가시 사용하는 API " + "channelId, subChannelId 로 소켓연결 후 구독시 헤더에 변수명(username)으로 닉네임 + id(chatRoomId) 담아서 요청해주세요 ")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 (ownerId)방장 정보입니다. or 이미 사용중인 닉네임입니다.",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomJoinResponseDto>> joinChatRoom(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id,
            @Valid @RequestBody ChatRoomJoinRequestDto chatRoomJoinRequestDto
            ) {

        ChatRoomJoinResponseDto responseDto = chatRoomService.joinChatRoom(id,chatRoomJoinRequestDto);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.OK);
    }

    @PatchMapping("/{id}/role/{memberId}")
    @Operation(summary = "팀변경 API", description = "팀변경시 사용하는 API " + "원래 subChannelID 구독해제 및 response 값인 subChannelID 으로 재구독 부탁")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 request dto , 변경하고자 하는 팀의 인원 제한 등",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomTeamChangeResponseDto>> changeTeam(
            @PathVariable("memberId")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "Invalid UUID format") String memberId,
            @PathVariable("id") String id,
            @Valid @RequestBody ChatRoomTeamChangeRequestDto chatRoomTeamChangeRequestDto) {

        ChatRoomTeamChangeResponseDto responseDto = chatRoomService.changeTeam(id,memberId,chatRoomTeamChangeRequestDto);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto),HttpStatus.OK);
    }

    @PostMapping("/{id}/subscription/error")
    @Operation(summary = "구독실패 에러처리 API", description = "구독을 실패할 경우 롤백처리 후 응답해주는 API (응답오기 전까지 사용자에게 재시도 요청반환)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseSuccessDto.class))}),
            @ApiResponse(responseCode = "500", description = "fail",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto.CommonResponseSuccessDto> subscriptionFailed(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id,
            @Valid @RequestBody ChatRoomSubscriptionFailureDto chatRoomSubscriptionFailureDto
    ) {

        String message = chatRoomService.subscriptionFailed(id, chatRoomSubscriptionFailureDto);

        return new ResponseEntity<>(CommonResponseDto.CommonResponseSuccessDto.success(HttpStatus.OK.value(), message),HttpStatus.OK);
    }

    @PatchMapping("/{id}/description")
    @Operation(summary = "토론개요 수정 API", description = "토론을 개요(부제목)을 수정하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseSuccessDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto.CommonResponseSuccessDto> modifyDescription(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id,
            @Valid @RequestBody ChatRoomModifyDescriptionRequestDto chatRoomModifyDescriptionRequestDto
    ) {
        String message = chatRoomService.modifyDescription(id, chatRoomModifyDescriptionRequestDto);
        return new ResponseEntity<>(CommonResponseDto.CommonResponseSuccessDto.success(HttpStatus.OK.value(), message),HttpStatus.OK);
    }


    @PatchMapping("/{id}/home")
    @Operation(summary = "home 버튼 클릭시 발생하는 API", description = "home 버튼 GET 요청하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomHomeResponseDto>> getDescription(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id
    ) {
        ChatRoomHomeResponseDto responseDto = chatRoomService.getDescription(id);
        return new ResponseEntity<>(CommonResponseDto.success(responseDto),HttpStatus.OK);
    }


    @GetMapping("/{id}/participants")
    @Operation(summary = "참가자 인원 수 GET 요청하는 API", description = "현재 채팅방에 참여중인 인원 수를 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomParticipantsCountDto>> getParticipantCounts(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id
    ) {

        ChatRoomParticipantsCountDto responseDto = chatRoomService.getParticipantCounts(id);
        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    @Operation(summary = "채팅방 정보 GET 요청하는 API", description = "채팅방 정보들을 반환합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomInfoResponseDto>> getChatRoomInfo(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id
            ) {

        ChatRoomInfoResponseDto responseDto = chatRoomService.getChatRoomInfo(id);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.OK);
    }

    @PatchMapping("/{id}/settings")
    @Operation(summary = "채팅방 설정 변경하는 API", description = "채팅 설정 변경을 요청하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseSuccessDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto.CommonResponseSuccessDto> changeChatRoomSettings(
            @PathVariable("id")
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format") String id,
            @Valid @RequestBody ChatRoomChangeSettingsRequestDto chatRoomChangeSettingsRequestDto
    ) {
        String message = chatRoomService.changeChatRoomSettings(id, chatRoomChangeSettingsRequestDto);

        return new ResponseEntity<>(CommonResponseDto.CommonResponseSuccessDto.success(200, message), HttpStatus.OK);
    }

    @GetMapping("/{id}/messages")
    @Operation(summary = "채팅방 이전 메시지 불러오는 API", description = "메인 채팅방 이전 메시지들을 불러와 이전 메시지 리스트를 반환")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "fail",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.CommonResponseErrorDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomBeforeMessagesResponseDto>> getBeforeMessages(
            @RequestParam("channelId")
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "Invalid UUID format") String channelId,
            @RequestParam("before")
            ZonedDateTime before
    ) {
        ChatRoomBeforeMessagesResponseDto responseDto = chatRoomService.getBeforeMessages(channelId, before);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.OK);
    }



}
