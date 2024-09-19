package server.cubeTalk.chat.handler;

import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SubscriptionManager {
    // 구독 상태를 관리하는 맵 (세션 ID -> 구독 채널 리스트)
    private final ConcurrentHashMap<String, Set<String>> subscriptionMap = new ConcurrentHashMap<>();

    // 구독 요청이 들어오면 구독 상태를 저장
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
    }
}

