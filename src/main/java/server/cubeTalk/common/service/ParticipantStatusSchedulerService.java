package server.cubeTalk.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.ChatRoomParticipantsListResponseDto;
import server.cubeTalk.chat.model.dto.ProgressInterruptionResponse;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.common.dto.CommonResponseDto;
import server.cubeTalk.member.model.entity.Member;
import server.cubeTalk.member.repository.MemberRepository;

import java.util.List;
import java.util.stream.Collectors;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantStatusSchedulerService {
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessageSendingOperations messageSendingOperations;
    private final MemberRepository memberRepository;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);


    @Scheduled(fixedRate = 10000) // 10초마다 실행
    public void checkParticipantsDisconnectedStatus() throws IllegalArgumentException {
        List<ChatRoom> chatRoomList = chatRoomRepository.findByChatStatus("STARTED");
        String originMemberId = null;

        for (ChatRoom chatRoom : chatRoomList) {
            /* 찬반 모드인 토론일 때 참가자 인원 확인 */
            if (chatRoom.getChatMode().equals("찬반")) {
                long supportCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("찬성")).count();
                long oppositeCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("반대")).count();

                if (supportCount == 0 || oppositeCount == 0) {
                    ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption","참가자가 없어 10초 뒤 채팅이 종료됩니다.");
                    log.info("채팅방 종료 이유 : 찬성,반대 인원 조건 부적 ");
                    messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);

                    // 10초 후에 채팅방 삭제
                    scheduler.schedule(() -> {
                        try {
                            chatRoomRepository.delete(chatRoom);
                            for (Participant participant : chatRoom.getParticipants()) {
                                memberRepository.deleteByMemberId(participant.getMemberId());
                            }
                        } catch (Exception e) {
                            log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
                        }
                    }, 10, TimeUnit.SECONDS);


                    throw new IllegalArgumentException("참가자가 없어 채팅이 종료됩니다.");

                }


            } /* 자유 모드인 토론일 때 참가자 인원 확인 */
            else if (chatRoom.getChatMode().equals("자유")) {
                long participantsCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("자유")).count();
                if (participantsCount == 0) {
                    ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption","참가자가 없어 10초 뒤 채팅이 종료됩니다.");
                    messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);

                    // 10초 후에 채팅방 삭제
                    scheduler.schedule(() -> {
                        try {
                            chatRoomRepository.delete(chatRoom);
                            for (Participant participant : chatRoom.getParticipants()) {
                                memberRepository.deleteByMemberId(participant.getMemberId());
                            }
                        } catch (Exception e) {
                            log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
                        }
                    }, 10, TimeUnit.SECONDS);

                    throw new IllegalArgumentException("참가자가 없어 채팅이 종료됩니다.");
                }

            }

            /* DISCONNECTED 된 참가자 상태 관리 */
            List<Participant> disconnectedParticipants = chatRoom.getParticipants().stream()
                    .filter(participant -> participant.getStatus().equals("DISCONNECTED"))
                    .toList();

            if (!disconnectedParticipants.isEmpty()) {
                String disconnectedNickName = null;

                // 방장 확인
                boolean isOwnerDisconnected = disconnectedParticipants.stream()
                        .anyMatch(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));

                if (isOwnerDisconnected) {
                    // 새로운 방장 후보군 필터링 (DISCONNECTED 및 관전 제외)
                    List<Participant> availableParticipants;

                    if (chatRoom.getChatMode().equals("찬반")) {
                        availableParticipants = chatRoom.getParticipants().stream()
                                .filter(participant -> !participant.getStatus().equals("DISCONNECTED") && !participant.getRole().equals("관전"))
                                .toList();
                    } else {
                        availableParticipants = chatRoom.getParticipants().stream()
                                .filter(participant -> !participant.getStatus().equals("DISCONNECTED"))
                                .toList();
                    }

                    if (!availableParticipants.isEmpty()) {

                        // 기존 방장 정보 추출
                        Participant originOwner = disconnectedParticipants.stream()
                                .filter(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()))
                                .findAny()
                                .orElseThrow(() -> new IllegalArgumentException("기존 방장이 없습니다."));

                        disconnectedNickName = originOwner.getNickName();

                        // 메인 채팅방에서 기존 방장 제거
                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));

                        // 서브 채팅방에서 기존 방장 제거
                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                            if (subChatRoom.getType().equals(originOwner.getRole())) {
                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));
                            }
                        }

                        // 새로운 방장 설정
                        Participant newOwner = availableParticipants.get(0).toBuilder()
                                .status("OWNER")
                                .build();

                        // 기존 임시 방장 정보 제거
                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(newOwner.getMemberId()));
                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                            if (subChatRoom.getType().equals(newOwner.getRole())) {
                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(newOwner.getMemberId()));
                            }
                        }

                        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                                .ownerId(newOwner.getMemberId())
                                .build();

                        // 메인 채팅방에 새 방장 추가
                        updatedChatRoom.getParticipants().add(newOwner);

                        // 서브 채팅방에 새로운 방장 추가
                        for (SubChatRoom subChatRoom : updatedChatRoom.getSubChatRooms()) {
                            if (subChatRoom.getType().equals(newOwner.getRole())) {
                                subChatRoom.getParticipants().add(newOwner);
                            }
                        }


                        log.info("Disconnected된 기존 방장의 닉네임: {}", disconnectedNickName);

                        messageSendingOperations.convertAndSend("/topic/chat."+chatRoom.getChannelId(), disconnectedNickName + "님이 나갔습니다.");
                        originMemberId = originOwner.getMemberId();
                        chatRoomRepository.save(updatedChatRoom);

                    } else { /* 방장 후보군이 없는 경우 */

                        // 채팅팡 폭파 10초뒤
                        ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption","방장후보군이 없어 10초 뒤 채팅이 종료됩니다.");
                        messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);

                        // 10초 후에 채팅방 삭제
                        scheduler.schedule(() -> {
                            try {
                                chatRoomRepository.delete(chatRoom);
                                for (Participant participant : chatRoom.getParticipants()) {
                                    memberRepository.deleteByMemberId(participant.getMemberId());
                                }
                            } catch (Exception e) {
                                log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
                            }
                        }, 10, TimeUnit.SECONDS);
                    }
                }
                else {
                    // 방장이 아닌 경우
                    for (Participant disconnectedParticipant : disconnectedParticipants) {
                        disconnectedNickName = disconnectedParticipant.getNickName();
                        originMemberId = disconnectedParticipant.getMemberId();
                        // 메인 채팅방에서 해당 참가자 제거
                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(disconnectedParticipant.getMemberId()));

                        // 서브 채팅방에서 해당 참가자 제거
                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                            if (subChatRoom.getType().equals(disconnectedParticipant.getRole())) {
                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(disconnectedParticipant.getMemberId()));
                            }
                        }
                    }


                    messageSendingOperations.convertAndSend("/topic/chat."+chatRoom.getChannelId(), disconnectedNickName + "님이 나갔습니다.");

                    chatRoomRepository.save(chatRoom);

                }

                // member 삭제
                memberRepository.deleteByMemberId(originMemberId);

                // 퇴출시 참가자 목록 업데이트 후 전송
                List<ChatRoomParticipantsListResponseDto> responseDto = chatRoom.getParticipants().stream()
                        .map(participant -> new ChatRoomParticipantsListResponseDto(
                                participant.getNickName(),
                                participant.getRole(),
                                participant.getStatus()
                        ))
                        .collect(Collectors.toList());

                messageSendingOperations.convertAndSend("/topic/"+chatRoom.getId() + ".participants.list", CommonResponseDto.success(responseDto));

            }
        }
    }
}
