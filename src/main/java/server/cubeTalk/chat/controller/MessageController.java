package server.cubeTalk.chat.controller;


import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import server.cubeTalk.chat.handler.SubscriptionManager;
import server.cubeTalk.chat.model.dto.*;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.chat.service.ChatRoomService;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.List;


@Controller
@RequiredArgsConstructor
@Log4j2
public class MessageController {

    private final ChatRoomService chatRoomService;
    private final SubscriptionManager subscriptionManager;

    /*
      /pub/메세지 발행
      /topic/ 구독
     */

    @MessageMapping("/message/{channelId}")
    @SendTo("/topic/chat.{channelId}")
    public ChatRoomSendMessageResponseDto sendChatMessage(
            @DestinationVariable
            @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
                    message = "Invalid UUID format") String channelId,
            @Payload @Valid ChatRoomSendMessageRequestDto RequestDto, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("nickName", RequestDto.getSender());
        String sessionId = headerAccessor.getSessionId();

        // 구독 상태 검증
        if (!subscriptionManager.isSubscribed(sessionId, channelId)) {
            throw new IllegalArgumentException("구독되지 않은 채널에 메시지를 발행할 수 없습니다.");
        }
        log.info("메시지 받기 ");
        subscriptionManager.printSubscriptions();
        ChatRoomSendMessageResponseDto responseDto = chatRoomService.sendChatMessage(channelId,RequestDto);

        return responseDto;
    }

    @MessageMapping("/{id}/ready")
    @SendTo("/topic/{id}.participants.list")
    public CommonResponseDto<List<ChatRoomParticipantsListResponseDto>> sendParticipantsList(
            @DestinationVariable
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format")
            String id,
            @Payload @Valid ChatRoomReadyStatusRequestDto chatRoomReadyStatusRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // 구독 상태 검증
        if (!subscriptionManager.isSubscribed(sessionId, id + ".participants.list")) {
            throw new IllegalArgumentException("구독되지 않은 채널에 메시지를 발행할 수 없습니다.");
        }

        List<ChatRoomParticipantsListResponseDto> responseDto = chatRoomService.sendParticipantsList(id,chatRoomReadyStatusRequestDto);

        return CommonResponseDto.success(responseDto);
    }

    @MessageMapping("/{id}/vote")
    public void voteChat(
            @DestinationVariable
            @Pattern(regexp = "^[a-fA-F0-9]{24}$",
                    message = "Invalid UUID format")
            String id,
            @Payload @Valid ChatRoomVoteRequestDto chatRoomVotesRequestDto, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();

        // 구독 상태 검증
        if (!subscriptionManager.isSubscribed(sessionId, "progress." +id )) {
            throw new IllegalArgumentException("구독되지 않은 채널에 메시지를 발행할 수 없습니다.");
        }
        subscriptionManager.printSubscriptions();

        chatRoomService.voteChat(id,chatRoomVotesRequestDto);
    }



}
