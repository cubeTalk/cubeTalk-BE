package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.handler.SubscriptionManager;
import server.cubeTalk.chat.model.dto.ChatRoomProgressResponseDto;
import server.cubeTalk.chat.model.dto.ChatRoomVoteResultResponseDto;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.DebateSettings;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class WebSocketService {
    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRoomRepository chatRoomRepository;
    private final SubscriptionManager subscriptionManager;
    private boolean isVoteEnd = false;
    public void sendErrorMessage(String title, String errorMessage) {

        messagingTemplate.convertAndSend("/topic/error", CommonResponseDto.CommonResponseSocketErrorDto.error(title,errorMessage));
    }

    /* 채팅방 진행 */
    public void progressChatRoom(ChatRoom chatRoom, SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        String id = chatRoom.getId();

        // 구독 상태 검증
        if (!subscriptionManager.isSubscribed(sessionId, "progress." +id )) {
            throw new IllegalArgumentException("구독되지 않은 채널에 메시지를 발행할 수 없어 채팅방 진행이 어렵습니다.");
        }

        DebateSettings debateSettings = chatRoom.getDebateSettings();
        double chatDuration = chatRoom.getChatDuration();

        if (chatRoom.getChatMode().equals("찬반")) {
            AtomicLong totalDurationInSeconds = new AtomicLong((long) (chatDuration * 60));

            // 단계별 타이머 스케줄링
            startPhase("positiveEntry", TimeUnit.MINUTES.toSeconds(debateSettings.getPositiveEntry()), totalDurationInSeconds, id, debateSettings);
        }
    }

    private void startPhase(String phase, long duration, AtomicLong totalDurationInSeconds, String id, DebateSettings debateSettings) {
        // 새로운 스케줄러를 각 단계마다 생성
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        AtomicLong phaseDuration = new AtomicLong(duration);

        scheduler.scheduleAtFixedRate(() -> {
            long remainingPhaseSeconds = phaseDuration.decrementAndGet();

            String remainingTime = formatDuration(remainingPhaseSeconds);

            // 현재 단계 상태를 전송
            ChatRoomProgressResponseDto.ChatRoomBasicProgressResponse progressResponse =
                    ChatRoomProgressResponseDto.ChatRoomBasicProgressResponse.progress(phase, remainingTime, "타이머 전송 중..");
            messagingTemplate.convertAndSend("/topic/progress." + id, progressResponse);

            if (remainingPhaseSeconds <= 0) {
                scheduler.shutdown(); // 현재 단계 타이머 종료
                System.out.println(phase + " 완료됨, 다음 단계로 이동");

                // 다음 단계로 이동
                switch (phase) {
                    case "positiveEntry":
                        startPhase("negativeQuestioning", TimeUnit.MINUTES.toSeconds(debateSettings.getNegativeQuestioning()), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "negativeQuestioning":
                        startPhase("negativeEntry", TimeUnit.MINUTES.toSeconds(debateSettings.getNegativeEntry()), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "negativeEntry":
                        startPhase("positiveQuestioning", TimeUnit.MINUTES.toSeconds(debateSettings.getPositiveQuestioning()), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "positiveQuestioning":
                        startPhase("positiveRebuttal", TimeUnit.MINUTES.toSeconds(debateSettings.getPositiveRebuttal()), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "positiveRebuttal":
                        startPhase("negativeRebuttal", TimeUnit.MINUTES.toSeconds(debateSettings.getNegativeRebuttal()), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "negativeRebuttal":
                        startPhase("votingTime", TimeUnit.SECONDS.toSeconds((long) (debateSettings.getVotingTime() * 60)), totalDurationInSeconds, id, debateSettings);
                        break;
                    case "votingTime":
                        // 투표 종료 처리
                        isVoteEnd = true;
                        sendFinalResults(id);
                        break;
                }
            }
        }, 0, 1, TimeUnit.SECONDS);
    }
    // 투표 결과와 MVP를 계산하여 전송하는 함수
    public void sendFinalResults(String chatRoomId) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(()-> new IllegalArgumentException("해당 채팅방이 존재하지 않습니다."));

        int supportVotes = chatRoom.getVote().getSupport();
        int oppositeVotes = chatRoom.getVote().getOpposite();
        String mvp = chatRoom.getVote().calculateMVP();

        ChatRoomVoteResultResponseDto result = new ChatRoomVoteResultResponseDto(supportVotes, oppositeVotes, mvp);

        ChatRoomProgressResponseDto<ChatRoomVoteResultResponseDto> finalMessage = new ChatRoomProgressResponseDto<>(
                "result",
                "00:00:00",
                "Timer has ended.",
                result
        );

        if (isVoteEnd) {
            ChatRoom endedChatRoom = chatRoom.toBuilder()
                    .chatStatus("ENDED")
                    .build();
            chatRoomRepository.save(endedChatRoom);
            /* 추후 db 삭제 및 멤버삭제 */
            messagingTemplate.convertAndSend("/topic/progress." + chatRoomId, finalMessage);
        }
    }

    /* 시간 포맷팅 함수 */
    private String formatDuration(long totalSeconds) {
        long hours = TimeUnit.SECONDS.toHours(totalSeconds);
        long minutes = TimeUnit.SECONDS.toMinutes(totalSeconds) - (hours * 60);
        long seconds = totalSeconds - (minutes * 60) - (hours * 3600);
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }


}
