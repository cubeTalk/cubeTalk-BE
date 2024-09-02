package server.cubeTalk.chat.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Controller;
import server.cubeTalk.chat.model.entity.Message;

import java.time.LocalDateTime;

@Controller
@RequiredArgsConstructor
public class MessageController {

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    /*
      /pub/message 메세지 발행
      /topic/{channelId} 구독
     */

    @MessageMapping("/message")
    public  void newUser(@Payload Message message, SimpMessageHeaderAccessor headerAccessor) {
        headerAccessor.getSessionAttributes().put("username", message.getSender());

        Message processedMessage = Message.builder()
                .type(message.getType())
                .sender(message.getSender())
                .channelId(message.getChannelId())
                .data(message.getData())
                .timestamp(LocalDateTime.now())
                .build();

        simpMessageSendingOperations.convertAndSend("/topic/" + processedMessage.getChannelId(), processedMessage);

    }
}
