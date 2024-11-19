package server.cubeTalk.chat.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;

import java.util.*;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Log4j2
public class SubscriptionManager {

    private final ChatRoomRepository chatRoomRepository;
    private final RedisTemplate<String,Object> redisTemplate;
    private static final String SUBSCRIPTION_KEY = "subscriptions";
    private final String SESSION_NICKNAME_KEY = "sessionNicknames";

    /* 구독 요청이 들어오면 구독 상태를 저장 */
    // 닉네임이 있을 때 사용하는 메서드
    public void addSubscription(String sessionId, String channelId, String nickName) {
        // 구독 채널 추가
        redisTemplate.opsForHash().put(SUBSCRIPTION_KEY, sessionId, channelId);

        // 닉네임이 있을 경우 추가
        if (nickName != null && !nickName.isEmpty()) {
            redisTemplate.opsForHash().put(SESSION_NICKNAME_KEY, sessionId, nickName);
        }
    }

    // 닉네임 없이 구독만 처리하는 메서드
    public void addSubscription(String sessionId, String channelId) {
        redisTemplate.opsForHash().put(SUBSCRIPTION_KEY, sessionId, channelId);
    }


    // 구독 해제 시 구독 상태에서 제거
    public void removeSubscription(String sessionId, String channelId) {
        redisTemplate.opsForHash().delete(SUBSCRIPTION_KEY, sessionId, channelId);
    }

    // 구독 여부를 확인
    public boolean isSubscribed(String sessionId, String channelId) {
        return redisTemplate.opsForHash().hasKey(SUBSCRIPTION_KEY, sessionId);
    }

    // 세션이 끊길 때 해당 세션의 모든 구독을 제거
    public void removeSession(String sessionId) {
        redisTemplate.opsForHash().delete(SUBSCRIPTION_KEY, sessionId);
        redisTemplate.opsForHash().delete(SESSION_NICKNAME_KEY, sessionId);
    }


    public void printSubscriptions() {
        Map<Object, Object> subscriptions = redisTemplate.opsForHash().entries("subscriptions");

        subscriptions.forEach((sessionId, channels) -> {
            System.out.println("Session: " + sessionId + " -> Channels: " + channels);
        });
    }


    public Set<String> searchUUIDChannels(String sessionId) {
        Set<String> uuidChannels = new HashSet<>();
        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        Pattern pattern = Pattern.compile(uuidPattern);

        List<Object> channels = redisTemplate.opsForHash().values(SUBSCRIPTION_KEY);
        for (Object channel : channels) {
            if (pattern.matcher(channel.toString()).matches()) {
                uuidChannels.add(channel.toString());
            }
        }
        return uuidChannels;
    }

    public boolean isNickNameInList(List<String> nickNames) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("sessionNicknames");

        return entries.values().stream()
                .anyMatch(nickNames::contains); // nickNames 중 하나라도 포함되어 있는지 확인
    }


    /* 해당 channelId로 구독된 채팅방을 찾는 메서드 */
    public ChatRoom searchChatRoom(Set<String> channelIds) {
        for (String channelId : channelIds) {
            ChatRoom chatRoom = chatRoomRepository.findByChannelId(channelId);
            if (chatRoom != null) {
                return chatRoom;
            }
        }
        return null;
    }

    /* sessionId로 nicnName 반환하는 메서드 */
    public String searchNickName(String sessionId) {
        Object nickName = redisTemplate.opsForHash().get(SESSION_NICKNAME_KEY, sessionId);
        return nickName != null ? nickName.toString() : null;
    }

    /* nickName으로 sessionId를 반환하는 메서드 */
    public Optional<String> searchSessionIdByNickName(String nickName) {
        // Redis Hash에서 모든 엔트리를 가져와 필터링
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("sessionNicknames");
        return entries.entrySet().stream()
                .filter(entry -> nickName.equals(entry.getValue())) // nickName이 일치하는지 확인
                .map(entry -> (String) entry.getKey()) // sessionId 반환
                .findFirst(); // 첫 번째 일치하는 값만 반환
    }

}

