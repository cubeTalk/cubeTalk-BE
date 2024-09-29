package server.cubeTalk.common.controller;

import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class WebSocketErrorHandler {

    @MessageExceptionHandler
    public void handleException(Throwable exception) {
        throw new IllegalArgumentException("에러 발생: " + exception.getMessage());
    }
}

