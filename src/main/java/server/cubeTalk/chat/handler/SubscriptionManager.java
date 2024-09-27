package server.cubeTalk.chat.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Log4j2
public class SubscriptionManager {

    private final ChatRoomRepository chatRoomRepository;
    // 구독 상태를 관리하는 맵 (세션 ID -> 구독 채널 리스트)
    private final ConcurrentHashMap<String, Set<String>> subscriptionMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, String> sessionNickNameMap = new ConcurrentHashMap<>();

    /* 구독 요청이 들어오면 구독 상태를 저장 */
    // 닉네임이 있을 때 사용하는 메서드
    public void addSubscription(String sessionId, String channelId, String nickName) {
        subscriptionMap.computeIfAbsent(sessionId, k -> new HashSet<>()).add(channelId);

        if (nickName != null && !nickName.isEmpty()) {
            sessionNickNameMap.put(sessionId, nickName);
        }
    }

    // 닉네임 없이 구독만 처리하는 메서드
    public void addSubscription(String sessionId, String channelId) {
        subscriptionMap.computeIfAbsent(sessionId, k -> new HashSet<>()).add(channelId);
    }


    // 구독 해제 시 구독 상태에서 제거
    public void removeSubscription(String sessionId, String channelId) {
        Set<String> subscriptions = subscriptionMap.get(sessionId);
        if (subscriptions != null) {
            subscriptions.remove(channelId);
        }
    }

    // 구독 여부를 확인
    public boolean isSubscribed(String sessionId, String channelId) {
        return subscriptionMap.containsKey(sessionId) && subscriptionMap.get(sessionId).contains(channelId);
    }

    // 세션이 끊길 때 해당 세션의 모든 구독을 제거
    public void removeSession(String sessionId) {
        subscriptionMap.remove(sessionId);
        sessionNickNameMap.remove(sessionId);
    }


    public void printSubscriptions() {
        subscriptionMap.forEach((sessionId, channels) ->
                System.out.println("Session: " + sessionId + " -> Channels: " + channels)
        );
    }

    public Set<String> searchUUIDChannels(String sessionId) {
        Set<String> channels = subscriptionMap.getOrDefault(sessionId, new HashSet<>());

        String uuidPattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";
        Pattern pattern = Pattern.compile(uuidPattern);

        // UUID에 해당하는 채널만 필터링
        Set<String> uuidChannels = new HashSet<>();
        for (String channel : channels) {
            if (pattern.matcher(channel).matches()) {
                uuidChannels.add(channel);
            }
        }

        return uuidChannels;
    }

    public boolean isNickNameInList(List<String> nickNames) {
        final boolean[] isNickNameFound = {false};

        subscriptionMap.forEach((sessionId, channels) -> {
            String nickName = sessionNickNameMap.get(sessionId); // sessionNickNameMap에서 sessionId로 닉네임 가져오기
            System.out.println("Session: " + sessionId + " -> Channels: " + channels);

            if (nickName != null && !nickName.isEmpty() && nickNames.contains(nickName)) {
                isNickNameFound[0] = true;
            }
        });

        return isNickNameFound[0];
    }

    /* 해당 channelId로 구독된 채팅방을 찾는 메서드 */
    public ChatRoom searchChatRoom(Set<String> channelId) {
        return chatRoomRepository.findByChannelId(channelId.iterator().next());
    }

    /* sessionId로 nicnName 반환하는 메서드 */
    public String searchNickName(String sessionId) {
        System.out.println(subscriptionMap);
        if (subscriptionMap.containsKey(sessionId)) {
            String nickName = sessionNickNameMap.get(sessionId);
            return nickName;
        }
        return null;
    }

    /* nickName으로 sessionId를 반환하는 메서드 */
    public Optional<String> searchSessionIdByNickName(String nickName) {
        return sessionNickNameMap.entrySet().stream()
                .filter(entry -> nickName.equals(entry.getValue()))  // nickName이 일치하는지 확인
                .map(Map.Entry::getKey)  // sessionId 반환
                .findFirst();  // 첫 번째 일치하는 값만 반환
    }



}

