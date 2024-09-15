package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.common.dto.CommonResponseDto;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    public void sendErrorMessage(String title, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/error", CommonResponseDto.CommonResponseSocketErrorDto.error(title,errorMessage));
    }

    /* 채팅방 진행 */
    public void progressChatRoom(ChatRoom chatRoom) {
        String id = chatRoom.getId();

        messagingTemplate.convertAndSend("/topic/progress." + id);
    }

}
