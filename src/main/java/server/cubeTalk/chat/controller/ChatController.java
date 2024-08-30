package server.cubeTalk.chat.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomCreateResponseDto;
import server.cubeTalk.chat.model.dto.ChatRoomJoinRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomJoinResponseDto;
import server.cubeTalk.chat.service.ChatRoomService;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.UUID;

@Tag(name = "채팅", description = "채팅 API")
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
            @RequestBody ChatRoomCreateRequestDto requestDto) {

            ChatRoomCreateResponseDto responseDto = chatRoomService.createChatRoom(requestDto);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.CREATED);
    }

    @PostMapping("/{channelId}/participants")
    @Operation(summary = "채팅방 참가 API", description = "채팅방 참가시 사용하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "success",
            content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))}),
            @ApiResponse(responseCode = "400", description = "유효하지 않은 (ownerId)방장 정보입니다.",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomJoinResponseDto>> joinChatRoom(
            @PathVariable String channelId,
            @RequestBody ChatRoomJoinRequestDto chatRoomJoinRequestDto
            ) {

        ChatRoomJoinResponseDto responseDto = chatRoomService.joinChatRoom(channelId,chatRoomJoinRequestDto);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.OK);
    }




}
