package server.cubeTalk.chat.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import server.cubeTalk.chat.model.dto.ChatRoomCommonMessageResponseDto;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.chat.service.WebSocketService;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Component
@RequiredArgsConstructor
@Log4j2
public class WebSocketChatEventListener {

    private final SimpMessageSendingOperations messageSendingOperations;
    private final ChatRoomRepository chatRoomRepository;
    private final SubscriptionManager subscriptionManager;
    private final WebSocketService webSocketService;

    @EventListener
    public void handleWebSocketConnectListener (SessionConnectedEvent event) {
        log.info("연결");
    }

    @EventListener
    public void handleWebSocketDisconnectListner(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());

        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();
        // 세션이 끊길 때 해당 세션의 모든 구독 상태 제거
        // 채팅인 경우 해당 세션 id, nickname을 얻어서 관련 db 제거
        log.info("연결끊긴={}" ,sessionId);
        subscriptionManager.printSubscriptions();

        try {
            String nativeHeaders = (String) headerAccessor.getHeader("id");
            if (nativeHeaders != null) {
                // 정상 종료 처리
                log.info("정상 구독 해제 처리됨.");
                messageSendingOperations.convertAndSend("/topic/error",
                        CommonResponseDto.CommonResponseSocketErrorDto.error("구독해제", destination + "이 구독해제되었습니다."));
            } else {
                // 비정상 종료 처리
                Set<String> channelIds = subscriptionManager.searchUUIDChannels(sessionId);
                log.info("channelIds: {}", channelIds);

                if (!channelIds.isEmpty()) {
                    ChatRoom chatRoom = subscriptionManager.searchChatRoom(channelIds);
                    log.info("chatRoom: {}", chatRoom);

                    if (chatRoom != null) {
                        String nickName = subscriptionManager.searchNickName(sessionId);
                        log.info("nickName: {}", nickName);

                        if (nickName != null) {
                            webSocketService.changeDisconnectParticipantStatus(chatRoom, nickName);
                        } else {
                            log.warn("닉네임을 찾을 수 없습니다. sessionId: {}", sessionId);
                        }
                    } else {
                        log.warn("채팅방을 찾을 수 없습니다. channelIds: {}", channelIds);
                    }
                } else {
                    log.warn("channelID 를 찾을 수 없습니다. sessionId: {}", sessionId);
                }
            }
        } catch (Exception e) {
            log.error("Error disconnect event listener: ", e);
        } finally {
            subscriptionManager.removeSession(sessionId);
        }
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        try {
            if (!subscriptionManager.isRedisConnected()) {
                log.error("Redis 연결 실패!");
                return;
            }
            log.info("구독중..");
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            System.out.println(headerAccessor.getMessageHeaders());
            String destination = headerAccessor.getDestination();
            String sessionId = headerAccessor.getSessionId();

            if (sessionId == null || destination == null) {
                throw new IllegalArgumentException("SessionId 또는 Destination이 null입니다");
            }

            if (destination.startsWith("/topic/chat.")) {

                String channelId = destination.substring("/topic/chat.".length());
                String nickName = headerAccessor.getFirstNativeHeader("nickName");
                String id = headerAccessor.getFirstNativeHeader("chatRoomId");

                if (nickName == null || id == null) throw new IllegalArgumentException("헤더값이 null입니다");
                ChatRoom chatRoom = chatRoomRepository.findById(id)
                        .orElseThrow(()-> new IllegalArgumentException("해댕 채팅방이 존재하지않습니다."));
                log.info("채팅 구독");


                if (!subscriptionManager.isSubscribed(sessionId,channelId)) {
                    try {
                        subscriptionManager.addSubscription(sessionId, channelId, nickName);
                        log.info("Subscription added - sessionId: {}, channelId: {}, nickName: {}",
                                sessionId, channelId, nickName);
                    } catch (Exception e) {
                        log.error("Redis 저장 중 오류 발생", e);
                        throw new IllegalStateException("구독 정보 저장 실패");
                    }

                    boolean isCheckDisconnectedStatus = chatRoom.getParticipants().stream().anyMatch(participant -> participant.getStatus().equals("DISCONNECTED"));
                    boolean isCheckDisconnectedNickName = chatRoom.getParticipants().stream().anyMatch(participant -> participant.getNickName().equals(nickName));

                    if (isCheckDisconnectedStatus && isCheckDisconnectedNickName) webSocketService.changeReconnectParticipantStatus(chatRoom,nickName);
                    log.info("재연결완료");
                    /* 메인 채팅방에 입장하는 경우 */
                    if (chatRoom.getChannelId().equals(channelId)) {


                    }
                    /* 서브 채팅방에 입장하는 경우 */
                    else {
                        try {
                            chatRoom.getSubChatRooms().stream()
                                    .filter(subChatRoom -> subChatRoom.getSubChannelId().equals(channelId))
                                    .findFirst()
                                    .map(SubChatRoom::getSubChannelId)
                                    .orElseThrow(() -> new IllegalArgumentException("서브 채팅방이 존재하지 않습니다."));

                        } catch (IllegalArgumentException e) {
                            messageSendingOperations.convertAndSend("/topic/error", CommonResponseDto.CommonResponseSocketErrorDto.error("구독실패",e.getMessage()));
                        }

                    }
                }

            }
            else {
                /* 채팅방 목적지 외 처리 */
                String channelId = destination.substring("/topic/".length());
                log.info("채팅방 외 구독 시도중...");

                try {
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
                } catch (Exception e) {
                    log.error("Redis 저장 중 오류 발생", e);
                    throw new IllegalStateException("구독 정보 저장 실패");
                }

            }

        } catch (IllegalArgumentException e) {
            log.error("구독 실패: " + e.getMessage());
            throw e;
        }
        catch (Exception e) {
            e.printStackTrace();
            throw e;
        }
    }
    @MessageExceptionHandler(IllegalArgumentException.class)
    public void handleException(IllegalArgumentException e) {
        throw e;
    }

    @EventListener
    public void  handleSessionUnSubscribeEvent(SessionUnsubscribeEvent event) {
        try {
            StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
            String sessionId = headerAccessor.getSessionId();
            String channelId = headerAccessor.getFirstNativeHeader("channelId");

            log.info("구독 해제 요청 - sessionId: {}, channelId: {}", sessionId, channelId);

            if (sessionId != null && channelId != null) {
                subscriptionManager.removeSubscription(sessionId, channelId);
                log.info("구독 해제 완료 - sessionId: {}, channelId: {}", sessionId, channelId);
            }
        } catch (Exception e) {
            log.error("구독 해제 처리 중 에러 발생", e);
        }

    }
}