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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomCreateResponseDto;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.UUID;

@Tag(name = "채팅", description = "채팅 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/chat")
public class ChatController {

    @PostMapping("/create")
    @Operation(summary = "채팅방 생성 API", description = "채팅방 생성시 사용하는 API")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "success",
                    content = {@Content(schema = @Schema(implementation = CommonResponseDto.class))})
    })
    public ResponseEntity<CommonResponseDto<ChatRoomCreateResponseDto>> createChatRoom(
            @RequestBody ChatRoomCreateRequestDto requestDto) {

        Long chatRoomId = 1L; // test 용
        String userId = UUID.randomUUID().toString(); // 고유 식별자 생성 (test용)

        ChatRoomCreateResponseDto responseDto = new ChatRoomCreateResponseDto(chatRoomId, userId);

        return new ResponseEntity<>(CommonResponseDto.success(responseDto), HttpStatus.CREATED);
    }

}
