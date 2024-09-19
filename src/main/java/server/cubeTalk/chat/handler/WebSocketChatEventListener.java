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
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketChatEventListener {

    private final SimpMessageSendingOperations messageSendingOperations;
    private final ChatRoomRepository chatRoomRepository;
    private final SubscriptionManager subscriptionManager;

    @EventListener
    public void handleWebSocketConnectListener (SessionConnectedEvent event) {
        log.info("연결");
    }

    @EventListener
    public void handleWebSocketDisconnectListner(SessionDisconnectEvent event) {
        String sessionId = StompHeaderAccessor.wrap(event.getMessage()).getSessionId();
        // 세션이 끊길 때 해당 세션의 모든 구독 상태 제거
        subscriptionManager.removeSession(sessionId);
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        try {
            log.info("구독중..");
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            System.out.println(headerAccessor.getMessageHeaders());
            String destination = headerAccessor.getDestination();
            String sessionId = headerAccessor.getSessionId();

            if (destination.startsWith("/topic/chat.")) {

                String channelId = destination.substring("/topic/chat.".length());
                String nickName = headerAccessor.getFirstNativeHeader("nickName");
                String id = headerAccessor.getFirstNativeHeader("chatRoomId");

                if (nickName == null || id == null) throw new IllegalArgumentException("헤더값이 null입니다");
                ChatRoom chatRoom = chatRoomRepository.findById(id)
                        .orElseThrow(()-> new IllegalArgumentException("해댕 채팅방이 존재하지않습니다."));
                log.info("채팅 구독");
                subscriptionManager.addSubscription(sessionId, channelId, nickName);
                /* 메인 채팅방에 입장하는 경우 */
                if (chatRoom.getChannelId().equals(channelId)) {
                    String message = nickName + "님이 입장하셨습니다.";
                    ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("ENTER",message);
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
                    subscriptionManager.addSubscription(sessionId, channelId, nickName);
                    String message = nickName + "님이 입장하셨습니다.";
                    ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("ENTER",message);
                    String jsonStringEnterMessage = new ObjectMapper().writeValueAsString(chatMessage);
                    messageSendingOperations.convertAndSend(destination, jsonStringEnterMessage);
                }
            }
            else {
                /* 채팅방 목적지 외 처리 */
                String channelId = destination.substring("/topic/".length());
                log.info("채팅방 외 구독");
                if (destination.startsWith("/topic/progress.")) {
                    String id = destination.substring("/topic/progress.".length());
                    chatRoomRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("progress.{id}에 해당하는 해당 채팅방이 존재하지 않습니다."));
                    subscriptionManager.addSubscription(sessionId, channelId);
                } else if (destination.startsWith("/topic/error")) {
                    subscriptionManager.addSubscription(sessionId, channelId);
                } else {
                    String id = destination.substring("/topic/".length(), destination.indexOf(".participants.list"));
                    chatRoomRepository.findById(id)
                            .orElseThrow(() -> new IllegalArgumentException("참여자 목록 구독에 해당하는 해당 채팅방이 존재하지 않습니다."));
                    subscriptionManager.addSubscription(sessionId, channelId);
                }

            }

        } catch (IllegalArgumentException e) {
            log.error("구독 실패: " + e.getMessage());
            messageSendingOperations.convertAndSend("/topic/error", CommonResponseDto.CommonResponseSocketErrorDto.error("구독실패",e.getMessage()));
        }
        catch (Exception e) {
            e.printStackTrace();

        }
    }

    @EventListener
    public void  handleSessionUnSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String id = headerAccessor.getFirstNativeHeader("channelId");
        subscriptionManager.removeSubscription(sessionId,id);

    }
}