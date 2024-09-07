package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import server.cubeTalk.common.dto.CommonResponseDto;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    public void sendErrorMessage(String channelId, String errorMessage) {
        messagingTemplate.convertAndSend("/topic/" + channelId, CommonResponseDto.fail(errorMessage));
    }

}
