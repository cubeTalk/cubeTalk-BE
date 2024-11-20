package server.cubeTalk.chat.handler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private static final String SUBSCRIPTION_KEY = "subscriptions";
    private final String SESSION_NICKNAME_KEY = "sessionNicknames";

    /* Redis 연결 상태 확인 */
    public boolean isRedisConnected() {
        try {
            return redisTemplate.getConnectionFactory().getConnection().ping() != null;
        } catch (Exception e) {
            log.error("Redis 연결 실패", e);
            return false;
        }
    }

    /* 닉네임이 있는 경우의 구독 처리 */
    public void addSubscription(String sessionId, String channelId, String nickName) {
        try {
            if (sessionId == null || channelId == null) {
                log.error("SessionId 또는 ChannelId가 null입니다");
                return;
            }

            // 기존 채널 목록 가져오기
            Set<String> channels = getChannels(sessionId);
            channels.add(channelId);

            // Set<String>을 JSON으로 변환하여 저장
            String channelsJson = objectMapper.writeValueAsString(channels);
            redisTemplate.opsForHash().put(SUBSCRIPTION_KEY, sessionId, channelsJson);
            log.info("구독 추가됨 - sessionId: {}, channels: {}", sessionId, channels);

            if (nickName != null && !nickName.isEmpty()) {
                redisTemplate.opsForHash().put(SESSION_NICKNAME_KEY, sessionId, nickName);
                log.info("닉네임 저장 - sessionId: {}, nickName: {}", sessionId, nickName);
            }
        } catch (Exception e) {
            log.error("구독 추가 중 에러 발생", e);
        }
    }

    /* 닉네임이 없는 경우의 구독 처리 */
    public void addSubscription(String sessionId, String channelId) {
        addSubscription(sessionId, channelId, null);
    }


    // 구독 해제 시 구독 상태에서 제거
    public void removeSubscription(String sessionId, String channelId) {
        try {
            Set<String> channels = getChannels(sessionId);
            channels.remove(channelId);

            if (channels.isEmpty()) {
                redisTemplate.opsForHash().delete(SUBSCRIPTION_KEY, sessionId);
            } else {
                String channelsJson = objectMapper.writeValueAsString(channels);
                redisTemplate.opsForHash().put(SUBSCRIPTION_KEY, sessionId, channelsJson);
            }
        } catch (Exception e) {
            log.error("구독 제거 중 에러 발생", e);
        }
    }

    // 구독 여부를 확인
    public boolean isSubscribed(String sessionId, String channelId) {
        Set<String> channels = getChannels(sessionId);
        return channels.contains(channelId);
    }

    private Set<String> getChannels(String sessionId) {
        try {
            String channelsJson = (String) redisTemplate.opsForHash().get(SUBSCRIPTION_KEY, sessionId);
            if (channelsJson != null) {
                return objectMapper.readValue(channelsJson, new TypeReference<HashSet<String>>() {});
            }
        } catch (Exception e) {
            log.error("채널 목록 조회 중 에러 발생", e);
        }
        return new HashSet<>();
    }

    // 세션이 끊길 때 해당 세션의 모든 구독을 제거
    public void removeSession(String sessionId) {
        try {
            if (sessionId != null) {
                // 두 개의 hash에서 각각 sessionId 항목 제거
                redisTemplate.opsForHash().delete(SUBSCRIPTION_KEY, sessionId);
                redisTemplate.opsForHash().delete(SESSION_NICKNAME_KEY, sessionId);
                log.debug("Successfully removed all data for sessionId: {}", sessionId);
            }
        } catch (Exception e) {
            log.error("Error removing session - sessionId: {}, error: {}", sessionId, e.getMessage());
        }
    }


    public void printSubscriptions() {
        Map<Object, Object> allSubscriptions = redisTemplate.opsForHash().entries(SUBSCRIPTION_KEY);
        allSubscriptions.forEach((sessionId, channelsJson) -> {
            try {
                Set<String> channels = objectMapper.readValue((String) channelsJson,
                        new TypeReference<HashSet<String>>() {});
                log.info("Session: {} -> Channels: {}", sessionId, channels);
            } catch (Exception e) {
                log.error("구독 정보 출력 중 에러 발생", e);
            }
        });
    }


    public Set<String> searchUUIDChannels(String sessionId) {
        Set<String> uuidChannels = new HashSet<>();
        if (sessionId == null) {
            return uuidChannels;
        }

        try {
            Set<String> channels = getChannels(sessionId);
            String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
            Pattern pattern = Pattern.compile(uuidPattern);

            for (String channel : channels) {
                if (pattern.matcher(channel).matches()) {
                    uuidChannels.add(channel);
                }
            }
        } catch (Exception e) {
            log.error("UUID 채널 검색 중 에러 발생", e);
        }

        return uuidChannels;
    }

    public boolean isNickNameInList(List<String> nickNames) {
        Map<Object, Object> entries = redisTemplate.opsForHash().entries("sessionNicknames");

        return entries.values().stream()
                .anyMatch(nickNames::contains);
    }


    /* 해당 channelId로 구독된 채팅방을 찾는 메서드 */
    public ChatRoom searchChatRoom(Set<String> channelIds) {
        if (channelIds == null || channelIds.isEmpty()) {
            log.debug("channelIds가 없습니다.");
            return null;
        }

        for (String channelId : channelIds) {
            try {
                ChatRoom chatRoom = chatRoomRepository.findByChannelId(channelId);
                if (chatRoom != null) {
                    log.debug("Found chat room for channelId {}: {}", channelId, chatRoom.getId());
                    return chatRoom;
                }
            } catch (Exception e) {
                log.error("Error searching chat room for channelId: " + channelId, e);
            }
        }
        log.debug("channel IDs에 해당하는 chatRoom이 없습니다.");
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
                .filter(entry -> nickName.equals(entry.getValue()))
                .map(entry -> (String) entry.getKey())
                .findFirst();
    }

}

