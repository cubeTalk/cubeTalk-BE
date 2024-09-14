package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import server.cubeTalk.common.dto.CommonResponseDto;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    public void sendErrorMessage(String title, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/error", CommonResponseDto.CommonResponseSocketErrorDto.error(title,errorMessage));
    }

}
