package server.cubeTalk.common.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.ChatRoomParticipantsListResponseDto;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.List;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Slf4j
public class ParticipantStatusSchedulerService {
    private final ChatRoomRepository chatRoomRepository;
    private final SimpMessageSendingOperations messageSendingOperations;

    @Scheduled(fixedRate = 60000) // 1분마다 실행
    public void checkParticipantsDisconnectedStatus() throws IllegalArgumentException {
        List<ChatRoom> chatRoomList = chatRoomRepository.findByChatStatus("STARTED");

        for (ChatRoom chatRoom : chatRoomList) {
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
                    List<Participant> availableParticipants = chatRoom.getParticipants().stream()
                            .filter(participant -> !participant.getStatus().equals("DISCONNECTED") && !participant.getRole().equals("관전"))
                            .toList();

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

                        chatRoomRepository.save(updatedChatRoom);

                    }
                }
                else {
                    // 방장이 아닌 경우
                    for (Participant disconnectedParticipant : disconnectedParticipants) {
                        disconnectedNickName = disconnectedParticipant.getNickName();
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
