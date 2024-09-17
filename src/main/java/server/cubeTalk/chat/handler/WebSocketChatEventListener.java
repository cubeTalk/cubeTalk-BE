package server.cubeTalk.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import server.cubeTalk.chat.model.dto.ChatRoomCommonMessageResponseDto;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketChatEventListener {

    private final SimpMessageSendingOperations messageSendingOperations;
    private final ChatRoomRepository chatRoomRepository;

    @EventListener
    public void handleWebSocketConnectListener (SessionConnectedEvent event) {
        log.info("연결");
    }

    @EventListener
    public void handleWebSocketDisconnectListner(SessionDisconnectEvent event) {}

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        try {
            log.info("구독중..");
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            System.out.println(headerAccessor.getMessageHeaders());
            String destination = headerAccessor.getDestination();

            if (destination.startsWith("/topic/chat.")) {

                String channelId = destination.substring("/topic/chat.".length());
                String nickName = headerAccessor.getFirstNativeHeader("nickName");
                String id = headerAccessor.getFirstNativeHeader("id");

                if (nickName == null || id == null) throw new IllegalArgumentException("헤더값이 null입니다");
                ChatRoom chatRoom = chatRoomRepository.findById(id)
                        .orElseThrow(()-> new IllegalArgumentException("해댕 채팅방이 존재하지않습니다."));
                log.info("채팅 구독");
                /* 메인 채팅방에 입장하는 경우 */
                if (chatRoom.getChannelId().equals(channelId)) {
                    String message = nickName + "님이 입장하셨습니다.";
                    ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto(message);
                    String jsonStringEnterMessage = new ObjectMapper().writeValueAsString(chatMessage);
                    messageSendingOperations.convertAndSend(destination, jsonStringEnterMessage);
                }
                /* 서브 채팅방에 입장하는 경우 */
                else  {
                    chatRoom.getSubChatRooms().stream()
                            .filter(subChatRoom -> subChatRoom.getSubChannelId().equals(channelId))
                            .findFirst()
                            .map(SubChatRoom::getSubChannelId)
                            .orElseThrow(() -> new IllegalArgumentException("서브 채팅방이 존재하지 않습니다."));
                    String message = nickName + "님이 입장하셨습니다.";
                    ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto(message);
                    String jsonStringEnterMessage = new ObjectMapper().writeValueAsString(chatMessage);
                    messageSendingOperations.convertAndSend(destination, jsonStringEnterMessage);
                }
            }
            else {
                /* 채팅방 목적지 외 처리 */
            }

    }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
}