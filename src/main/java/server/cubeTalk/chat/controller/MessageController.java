package server.cubeTalk.chat.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import server.cubeTalk.chat.model.dto.ChatRoomReadyStatusRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomParticipantsListResponseDto;
import server.cubeTalk.chat.service.ChatRoomService;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.List;


@Controller
@RequiredArgsConstructor
@Log4j2
public class MessageController {

    private final ChatRoomService chatRoomService;
    private final SimpMessageSendingOperations simpMessageSendingOperations;

    /*
      /pub/changeTeam 팀변경
      /pub/message 메세지 발행
      /topic/{channelId} 구독
     */

//    @MessageMapping("/message")
//    public  void newUser(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
//        headerAccessor.getSessionAttributes().put("username", message.getSender());
//
//        Message processedMessage = Message.builder()
//                .type(message.getType())
//                .sender(message.getSender())
//                .channelId(message.getChannelId())
//                .data(message.getData())
//                .timestamp(LocalDateTime.now())
//                .build();
//
//        simpMessageSendingOperations.convertAndSend("/topic/" + processedMessage.getChannelId(), processedMessage);
//
//    }

    @MessageMapping("/{id}/ready")
    @SendTo("/topic/{id}.participants.list")
    public CommonResponseDto<List<ChatRoomParticipantsListResponseDto>> sendParticipantsList(
            @DestinationVariable
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format")
            String id,
            @Payload @Valid ChatRoomReadyStatusRequestDto chatRoomReadyStatusRequestDto) {
        log.info("전송");
        List<ChatRoomParticipantsListResponseDto> responseDto = chatRoomService.sendParticipantsList(id,chatRoomReadyStatusRequestDto);
        log.info(responseDto);
        return CommonResponseDto.success(responseDto);
    }

}
