package server.cubeTalk.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.cubeTalk.chat.model.dto.ChatRoomParticipantsListResponseDto;
import server.cubeTalk.chat.model.dto.ProgressInterruptionResponse;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.chat.service.ChatRoomService;
import server.cubeTalk.chat.service.MessageService;
import server.cubeTalk.chat.service.WebSocketService;
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
    private final MessageService messageService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    @Transactional
    public void deleteChatRoom(ChatRoom chatRoom) {
        chatRoom.getParticipants().clear();
        chatRoom.getSubChatRooms().clear();
        chatRoomRepository.save(chatRoom);
        chatRoomRepository.delete(chatRoom);

    }
    @Transactional
    public void deleteMember(String memberId) {
        memberRepository.deleteByMemberId(memberId);
        log.info("삭제된 memberId {}",memberId);
    }


    public void checkParticipantsDisconnectedStatus() throws IllegalArgumentException {
        List<ChatRoom> chatRoomList = chatRoomRepository.findAll();
        for (ChatRoom chatRoom : chatRoomList) {
            /* 찬반 모드인 토론일 때 참가자 인원 확인 */
            if (chatRoom.getChatMode().equals("찬반")) {
                checkDebateParticipantsCount(chatRoom);
            } /* 자유 모드인 토론일 때 참가자 인원 확인 */
            else if (chatRoom.getChatMode().equals("자유")) {
                checkFreeParticipantsCount(chatRoom);
            }
        }
    }
    /* 자유 토론일 때 참가자 인원 확인 */
    public void checkFreeParticipantsCount(ChatRoom chatRoom) {
        long participantsCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("자유")).count();
        if (participantsCount == 0) {
            messageService.sendChatRoomMessage("EVENT","참가자가 없어 5초 뒤 채팅이 종료됩니다.","/topic/chat." + chatRoom.getChannelId());
            // 5초 후에 채팅방 삭제
            deleteChatRoomScheduler(chatRoom,5,"자유 채팅방 인원부족으로 인한");

            throw new IllegalArgumentException("참가자가 없어 채팅이 종료됩니다.");
        }

    }


    /* 찬반 토론일 때 참가자 인원 확인 */
    public void checkDebateParticipantsCount(ChatRoom chatRoom) {
        long supportCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("찬성")).count();
        long oppositeCount = chatRoom.getParticipants().stream().filter(p -> p.getRole().equals("반대")).count();

        if (supportCount == 0 || oppositeCount == 0) {

            log.info("채팅방 종료 이유 : 찬성,반대 인원 조건 부족 ");
            messageService.sendChatRoomMessage("EVENT","참가자가 없어 5초 뒤 채팅이 종료됩니다.","/topic/chat." + chatRoom.getChannelId());

            // 5초 후에 채팅방 삭제
            deleteChatRoomScheduler(chatRoom,5,"찬성,반대 인원 조건 부족");

            throw new IllegalArgumentException("참가자가 없어 채팅이 종료됩니다.");
        }
    }


    /* 채팅방 삭제 scheduler */
    public void deleteChatRoomScheduler(ChatRoom chatRoom,int delayInSeconds,String deleteReason) {
        scheduler.schedule(() -> {
            try {
                log.info(deleteReason + "으로 채팅방 삭제중..");
                deleteChatRoom(chatRoom);
                log.info("삭제완료");
                for (Participant participant : chatRoom.getParticipants()) {
                    deleteMember(participant.getMemberId());
                    log.info("모든 참가자 memberId 삭제 : {}",participant.getMemberId());
                }

            } catch (Exception e) {
                log.error("채팅방 삭제 중 오류 발생 : {}", e.getMessage());
            }
        }, delayInSeconds, TimeUnit.SECONDS);
    }

    /* 해당 참가자의 DISCONNECTED 상태를 확인 */
    public void scheduleStatusCheck(ChatRoom chatRoom, String userNickName) {
        log.info("{} 참가자의 disconnected를 확인 ",userNickName);
        scheduler.schedule(() -> {
            checkParticipantStatus(chatRoom, userNickName);
        }, 10, TimeUnit.SECONDS);
    }

    public void checkParticipantStatus(ChatRoom chatRoom, String nickName) {

        chatRoom = chatRoomRepository.findById(chatRoom.getId())
                .orElseThrow(() -> new IllegalArgumentException("채팅방을 찾을 수 없습니다."));

        Participant participant = chatRoom.getParticipants().stream()
                .filter(p -> p.getNickName().equals(nickName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("참가자를 찾을 수 없습니다."));

        log.info("최신 참가자 상태: {}", participant.getStatus());

        log.info("disconnected된 참가자의 시간: {}",participant.getDisconnectedUpdatedAt());
        log.info("disconnected된 참가자의 10초 뒤 시간: {}",participant.getDisconnectedUpdatedAt().plusSeconds(10));
        log.info("disconnected된 참가자의 현재 유지시간 : {}",DateTimeUtils.nowFromZone());

        if (participant.getStatus().equals("DISCONNECTED") && participant.getDisconnectedUpdatedAt().plusSeconds(10).isBefore(DateTimeUtils.nowFromZone())) {
            log.info("disconnected된 참가자 {} 님을 처리합니다.",nickName);
            handleStillDisconnected(chatRoom,participant);
        }else {
            log.info("참가자 {} 는 더 이상 DISCONNECTED 상태가 아닙니다. 강퇴 처리하지 않습니다.", nickName);
        }
    }

    @Transactional
    public void handleStillDisconnected(ChatRoom chatRoom,Participant participant) {
        // 상태가 여전히 DISCONNECTED일 때 수행할 추가 작업
        System.out.println(participant.getNickName() + "10초동안 DISCONNECTED 상태이기 때문에 강퇴처리합니다.");
        String originMemberId = participant.getMemberId();

        boolean isOwnerDisconnected = participant.getMemberId().equals(chatRoom.getOwnerId());
        if (isOwnerDisconnected) {
            // 새로운 방장 후보군 필터링 (DISCONNECTED 및 관전포함)
            List<Participant> availableParticipants = chatRoom.getParticipants().stream()
                    .filter(p -> !p.getStatus().equals("DISCONNECTED"))
                    .toList();
            if (availableParticipants.isEmpty()) {
                // 방장 후보군이 없는 경우
                // 메인,서브채팅방에서 기존 방장 제거
                removeOwnerMemberFromChatRoom(chatRoom,participant);

                messageService.sendChatRoomMessage("EVENT","방장후보군이 없어 5초 뒤 채팅이 종료됩니다.","/topic/chat." + chatRoom.getChannelId());

                if (chatRoom.getChatStatus().equals("CREATED")) {
                    checkParticipantsDisconnectedStatus();
                } else {}

            } else {
                // 메인,서브 채팅방에서 기존 방장 제거
                removeOwnerMemberFromChatRoom(chatRoom,participant);

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
                messageService.sendChatRoomMessage("EVENT",message,"/topic/chat." + chatRoom.getChannelId());
                chatRoomRepository.save(updatedChatRoom);

            }
        }
        else { // 방장이 아닌 경우

            // 메인,서브 채팅방에서 해당 참가자 제거
            removeMemberFromChatRoom(chatRoom,participant);

            chatRoomRepository.save(chatRoom);

            log.info("Disconnected된 참가자 닉네임: {}", participant.getNickName());

            String message =  participant.getNickName() + "님이 퇴장하셨습니다.";
            messageService.sendChatRoomMessage("EVENT",message,"/topic/chat." + chatRoom.getChannelId());

        }
        if (chatRoom.getChatStatus().equals("STARTED")) {
            checkParticipantsDisconnectedStatus();
        } else {
        }
        // member 삭제
        deleteMember(originMemberId);

        // 퇴출시 참가자 목록 업데이트 후 전송
        List<ChatRoomParticipantsListResponseDto> responseDto = chatRoom.getParticipants().stream()
                .map(p -> new ChatRoomParticipantsListResponseDto(
                        p.getNickName(),
                        p.getRole(),
                        p.getStatus()
                ))
                .collect(Collectors.toList());

        messageSendingOperations.convertAndSend("/topic/" + chatRoom.getId() + ".participants.list", CommonResponseDto.success(responseDto));
        log.info("퇴출후 참가자 목록 업데이트 전송");
    }

    /* 방장 제거 메서드 */
    public void removeOwnerMemberFromChatRoom(ChatRoom chatRoom,Participant participant) {
        chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));

        if (chatRoom.getChatMode().equals("찬반")) {
            for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                if (subChatRoom.getType().equals(participant.getRole())) {
                    subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(chatRoom.getOwnerId()));
                }
            }
        }
    }

    /* 참가자 제거 메서드 */
    public void removeMemberFromChatRoom(ChatRoom chatRoom, Participant participant) {
        chatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(participant.getMemberId()));

        if (chatRoom.getChatMode().equals("찬반")) {
            // 서브 채팅방에서 해당 참가자 제거
            for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
                if (subChatRoom.getType().equals(participant.getRole())) {
                    subChatRoom.getParticipants().removeIf(p -> p.getMemberId().equals(participant.getMemberId()));
                }
            }
        }
    }


}
