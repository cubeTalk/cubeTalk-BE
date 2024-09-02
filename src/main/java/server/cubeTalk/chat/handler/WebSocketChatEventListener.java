package server.cubeTalk.chat.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketChatEventListener {

    private final SimpMessageSendingOperations simpMessageSendingOperations;

    @EventListener
    public void handleWebSocketConnectListener (SessionConnectedEvent event) {
        log.info("연결");
    }

    @EventListener
    public void handleWebSocketDisconnectListner(SessionDisconnectEvent event) {}

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        try {
            log.info("구독");
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        System.out.println(headerAccessor.getMessageHeaders());

        String username = headerAccessor.getFirstNativeHeader("username");
        String channelId = headerAccessor.getDestination();
        System.out.println("username: " + username);

        if (username == null) {
            throw new IllegalArgumentException("헤더값 username이 null값입니다.");
        }

        if (username != null && channelId != null) {
            String message = username + "님이 입장하셨습니다.";
            simpMessageSendingOperations.convertAndSend(channelId, message);
        }
    }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}