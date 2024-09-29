package server.cubeTalk.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.cubeTalk.chat.model.dto.ChatRoomCommonMessageResponseDto;
import server.cubeTalk.chat.model.dto.ChatRoomParticipantsListResponseDto;
import server.cubeTalk.chat.model.dto.ProgressInterruptionResponse;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.common.dto.CommonResponseDto;
import server.cubeTalk.common.util.DateTimeUtils;
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

//    @Scheduled(fixedRate = 1000) // 1초마다 실행
    public void checkParticipantsDisconnectedStatus() throws IllegalArgumentException {
//        List<ChatRoom> chatRoomList = chatRoomRepository.findByChatStatus("STARTED");
        List<ChatRoom> chatRoomList = chatRoomRepository.findAll();
        for (ChatRoom chatRoom : chatRoomList) {
            /* 찬반 모드인 토론일 때 참가자 인원 확인 */
            if (chatRoom.getChatMode().equals("찬반")) {
                long supportCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("찬성")).count();
                long oppositeCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("반대")).count();

                if (supportCount == 0 || oppositeCount == 0) {
                    ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption","참가자가 없어 10초 뒤 채팅이 종료됩니다.");
                    log.info("채팅방 종료 이유 : 찬성,반대 인원 조건 부적 ");
                    messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);

                    // 5초 후에 채팅방 삭제
                    scheduler.schedule(() -> {
                        try {
                            chatRoomRepository.delete(chatRoom);
                            for (Participant participant : chatRoom.getParticipants()) {
                                memberRepository.deleteByMemberId(participant.getMemberId());
                            }
                        } catch (Exception e) {
                            log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
                        }
                    }, 5, TimeUnit.SECONDS);


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

        }
    }

    /* 해당 참가자의 DISCONNECTED 상태를 확인 */
    public void scheduleStatusCheck(ChatRoom chatRoom, String userNickName) {
        scheduler.schedule(() -> {
            checkParticipantStatus(chatRoom, userNickName);
        }, 10, TimeUnit.SECONDS);
    }

    public void checkParticipantStatus(ChatRoom chatRoom, String nickName) {

        Participant participant = chatRoom.getParticipants().stream()
                .filter(p -> p.getNickName().equals(nickName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

        if (participant.getStatus().equals("DISCONNECTED") && participant.getUpdatedAt().plusSeconds(10).isBefore(DateTimeUtils.nowFromZone())) {
            handleStillDisconnected(chatRoom,participant);
        }
    }

    @Transactional
    public void handleStillDisconnected(ChatRoom chatRoom,Participant participant) {
        // 상태가 여전히 DISCONNECTED일 때 수행할 추가 작업
        System.out.println(participant.getNickName() + "10초동안 DISCONNECTED 상태이기 때문에 강퇴처리합니다.");
        String originMemberId = participant.getMemberId();
        // 방장인지 확인
        // 방장이면 다른 참가자 리스트들 중에서 toss할 사람인지 확인하기 -> 없으면
        //
        // 방장이 아닌 경우
        // -> 채팅 시작됐는지 확인 -> 시작된 경우에는 해당 참가자 강퇴처리/  -> 시작 전이면 해당 참가자 강퇴
        boolean isOwnerDisconnected = participant.getMemberId().equals(chatRoom.getOwnerId());
        if (isOwnerDisconnected) {
            // 새로운 방장 후보군 필터링 (DISCONNECTED 및 관전포함)
            List<Participant> availableParticipants = chatRoom.getParticipants().stream()
                    .filter(p -> !p.getStatus().equals("DISCONNECTED"))
                    .toList();
            if (availableParticipants.isEmpty()) {
                // 방장 후보군이 없는 경우
                // 메인 채팅방에서 기존 방장 제거
                chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));

                // 서브 채팅방에서 기존 방장 제거
                if (chatRoom.getChatMode().equals("찬반")) {
                    for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                        if (subChatRoom.getType().equals(participant.getRole())) {
                            subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));
                        }
                    }
                }


                ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption", "방장후보군이 없어 5초 뒤 채팅이 종료됩니다.");
                messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);
                // 5초 후에 채팅방 삭제
//                scheduler.schedule(() -> {
//                    try {
//                        chatRoomRepository.delete(chatRoom);
//                        for (Participant p : chatRoom.getParticipants()) {
//                            memberRepository.deleteByMemberId(p.getMemberId());
//                        }
//                    } catch (Exception e) {
//                        log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
//                    }
//                }, 5, TimeUnit.SECONDS);

            } else {
                // 메인 채팅방에서 기존 방장 제거
                chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));

                // 서브 채팅방에서 기존 방장 제거
                if (chatRoom.getChatMode().equals("찬반")) {
                    for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                        if (subChatRoom.getType().equals(participant.getRole())) {
                            subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));
                        }
                    }
                }

                // 새로운 방장 설정
                Participant newOwner = availableParticipants.get(0).toBuilder()
                        .status("OWNER")
                        .build();

                // 기존 임시 방장 정보 제거
                chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(newOwner.getMemberId()));
                for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                    if (subChatRoom.getType().equals(newOwner.getRole())) {
                        subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(newOwner.getMemberId()));
                    }
                }

                ChatRoom updatedChatRoom = chatRoom.toBuilder()
                        .ownerId(newOwner.getMemberId())
                        .build();

                // 메인 채팅방에 새 방장 추가
                updatedChatRoom.getParticipants().add(newOwner);

                if (chatRoom.getChatMode().equals("찬반")) {
                    // 서브 채팅방에 새로운 방장 추가
                    for (SubChatRoom subChatRoom : updatedChatRoom.getSubChatRooms()) {
                        if (subChatRoom.getType().equals(newOwner.getRole())) {
                            subChatRoom.getParticipants().add(newOwner);
                        }
                    }
                }


                log.info("Disconnected된 기존 방장의 닉네임: {}", participant.getNickName());

                String message =  participant.getNickName() + "님이 퇴장하셨습니다.";
                ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("EVENT", message);
                messageSendingOperations.convertAndSend( "/topic/chat." + chatRoom.getChannelId(), chatMessage);
                chatRoomRepository.save(updatedChatRoom);

            }
        }
        else { // 방장이 아닌 경우

            // 메인 채팅방에서 해당 참가자 제거
            chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(participant.getMemberId()));

            if (chatRoom.getChatMode().equals("찬반")) {
                // 서브 채팅방에서 해당 참가자 제거
                for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                    if (subChatRoom.getType().equals(participant.getRole())) {
                        subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(participant.getMemberId()));
                    }
                }
            }

            chatRoomRepository.save(chatRoom);

            log.info("Disconnected된 참가자 닉네임: {}", participant.getNickName());

            String message =  participant.getNickName() + "님이 퇴장하셨습니다.";
            ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("EVENT", message);
            messageSendingOperations.convertAndSend( "/topic/chat." + chatRoom.getChannelId(), chatMessage);

        }
        checkParticipantsDisconnectedStatus();

        // member 삭제
        memberRepository.deleteByMemberId(originMemberId);

        // 퇴출시 참가자 목록 업데이트 후 전송
        List<ChatRoomParticipantsListResponseDto> responseDto = chatRoom.getParticipants().stream()
                .map(p -> new ChatRoomParticipantsListResponseDto(
                        p.getNickName(),
                        p.getRole(),
                        p.getStatus()
                ))
                .collect(Collectors.toList());

        messageSendingOperations.convertAndSend("/topic/" + chatRoom.getId() + ".participants.list", CommonResponseDto.success(responseDto));

    }





//    // DISCONNECTED 상태가 유지되었을 때 처리(채팅방 상태가 시작된 경우)
//    private void handleDisconnectedParticipantStarted(String memberId, String chatStatus) {
//        System.out.println("참가자 " + memberId + "를 처리합니다.");
//
//        List<ChatRoom> chatRoomList = chatRoomRepository.findByChatStatus(chatStatus);
//        String originMemberId = null;
//
//        for (ChatRoom chatRoom : chatRoomList) {
//            /* DISCONNECTED 된 참가자 상태 관리 */
//            List<Participant> disconnectedParticipants = chatRoom.getParticipants().stream()
//                    .filter(participant -> participant.getStatus().equals("DISCONNECTED"))
//                    .toList();
//
//            if (!disconnectedParticipants.isEmpty()) {
//                String disconnectedNickName = null;
//
//                // 방장 확인
//                boolean isOwnerDisconnected = disconnectedParticipants.stream()
//                        .anyMatch(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));
//
//                if (isOwnerDisconnected) {
//                    // 새로운 방장 후보군 필터링 (DISCONNECTED 및 관전포함)
//                    List<Participant> availableParticipants = chatRoom.getParticipants().stream()
//                            .filter(participant -> !participant.getStatus().equals("DISCONNECTED"))
//                            .toList();
//
//
//                    if (!availableParticipants.isEmpty()) {
//
//                        // 기존 방장 정보 추출
//                        Participant originOwner = disconnectedParticipants.stream()
//                                .filter(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()))
//                                .findAny()
//                                .orElseThrow(() -> new IllegalArgumentException("기존 방장이 없습니다."));
//
//                        disconnectedNickName = originOwner.getNickName();
//
//                        // 메인 채팅방에서 기존 방장 제거
//                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));
//
//                        // 서브 채팅방에서 기존 방장 제거
//                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
//                            if (subChatRoom.getType().equals(originOwner.getRole())) {
//                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(chatRoom.getOwnerId()));
//                            }
//                        }
//
//                        // 새로운 방장 설정
//                        Participant newOwner = availableParticipants.get(0).toBuilder()
//                                .status("OWNER")
//                                .build();
//
//                        // 기존 임시 방장 정보 제거
//                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(newOwner.getMemberId()));
//                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
//                            if (subChatRoom.getType().equals(newOwner.getRole())) {
//                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(newOwner.getMemberId()));
//                            }
//                        }
//
//                        ChatRoom updatedChatRoom = chatRoom.toBuilder()
//                                .ownerId(newOwner.getMemberId())
//                                .build();
//
//                        // 메인 채팅방에 새 방장 추가
//                        updatedChatRoom.getParticipants().add(newOwner);
//
//                        // 서브 채팅방에 새로운 방장 추가
//                        for (SubChatRoom subChatRoom : updatedChatRoom.getSubChatRooms()) {
//                            if (subChatRoom.getType().equals(newOwner.getRole())) {
//                                subChatRoom.getParticipants().add(newOwner);
//                            }
//                        }
//
//
//                        log.info("Disconnected된 기존 방장의 닉네임: {}", disconnectedNickName);
//
//                        messageSendingOperations.convertAndSend("/topic/chat." + chatRoom.getChannelId(), disconnectedNickName + "님이 나갔습니다.");
//                        originMemberId = originOwner.getMemberId();
//                        chatRoomRepository.save(updatedChatRoom);
//
//                    } else { /* 방장 후보군이 없는 경우 */
//
//                        // 채팅팡 폭파 10초뒤
//                        ProgressInterruptionResponse response = new ProgressInterruptionResponse("interruption", "방장후보군이 없어 10초 뒤 채팅이 종료됩니다.");
//                        messageSendingOperations.convertAndSend("/topic/progress." + chatRoom.getId(), response);
//
//                        // 10초 후에 채팅방 삭제
//                        scheduler.schedule(() -> {
//                            try {
//                                chatRoomRepository.delete(chatRoom);
//                                for (Participant participant : chatRoom.getParticipants()) {
//                                    memberRepository.deleteByMemberId(participant.getMemberId());
//                                }
//                            } catch (Exception e) {
//                                log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
//                            }
//                        }, 10, TimeUnit.SECONDS);
//                    }
//                } else {
//                    // 방장이 아닌 경우
//                    for (Participant disconnectedParticipant : disconnectedParticipants) {
//                        disconnectedNickName = disconnectedParticipant.getNickName();
//                        originMemberId = disconnectedParticipant.getMemberId();
//                        // 메인 채팅방에서 해당 참가자 제거
//                        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(disconnectedParticipant.getMemberId()));
//
//                        // 서브 채팅방에서 해당 참가자 제거
//                        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
//                            if (subChatRoom.getType().equals(disconnectedParticipant.getRole())) {
//                                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(disconnectedParticipant.getMemberId()));
//                            }
//                        }
//                    }
//
//
//                    chatRoomRepository.save(chatRoom);
//
//                }
//
//                String message = disconnectedNickName + "님이 퇴장하셨습니다.";
//                ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("EVENT", message);
//                messageSendingOperations.convertAndSend( "/topic/chat." + chatRoom.getChannelId(), chatMessage);
//
//
//
//                // member 삭제
//                memberRepository.deleteByMemberId(originMemberId);
//
//                // 퇴출시 참가자 목록 업데이트 후 전송
//                List<ChatRoomParticipantsListResponseDto> responseDto = chatRoom.getParticipants().stream()
//                        .map(participant -> new ChatRoomParticipantsListResponseDto(
//                                participant.getNickName(),
//                                participant.getRole(),
//                                participant.getStatus()
//                        ))
//                        .collect(Collectors.toList());
//
//                messageSendingOperations.convertAndSend("/topic/" + chatRoom.getId() + ".participants.list", CommonResponseDto.success(responseDto));
//
//            }
//
//        }
//    }
//


}
