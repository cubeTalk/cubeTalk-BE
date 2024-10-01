package server.cubeTalk.chat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.ChatRoomCommonMessageResponseDto;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final SimpMessageSendingOperations messageSendingOperations;
    /* 메세지 전달 */
    public void sendChatRoomMessage(String eventType, String messageContent, String destination) {
        ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto(eventType, messageContent);
        messageSendingOperations.convertAndSend(destination, chatMessage);
    }
}
