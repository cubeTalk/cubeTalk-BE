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

import java.util.UUID;

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

//    @PatchMapping("/{id}/role/{memberId}")
//    @Operation(summary = "팀변경 API", description = "팀변경시 사용하는 API " + "원래 subChannelID 구독해제 및 response 값인 subChannelID 으로 재구독 부탁")
//    @ApiResponses(value = {
//            @ApiResponse(responseCode = "200", description = "success",
//            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
//            @ApiResponse(responseCode = "400", description = "유효하지 않은 request dto , 변경하고자 하는 팀의 인원 제한 등",
//            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
//    })
//    public ResponseEntity<CommonResponseDto<ChatRoomTeamChangeResponseDto>> changeTeam(
//            @PathVariable("memberId")
//            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
//                    message = "Invalid UUID format") String memberId,
//            @PathVariable("id") String id,
//            @Valid @RequestBody ChatRoomTeamChangeRequestDto chatRoomTeamChangeRequestDto) {
//
//        ChatRoomTeamChangeResponseDto responseDto = chatRoomService.changeTeam(id,memberId,chatRoomTeamChangeRequestDto);
//
//        return new ResponseEntity<>(CommonResponseDto.success(responseDto),HttpStatus.OK);
//    }






}
