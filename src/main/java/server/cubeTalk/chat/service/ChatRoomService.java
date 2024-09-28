package server.cubeTalk.chat.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.cubeTalk.chat.handler.SubscriptionManager;
import server.cubeTalk.chat.model.dto.*;
import server.cubeTalk.chat.model.entity.*;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.chat.repository.MessageRepository;
import server.cubeTalk.common.util.DateTimeUtils;
import server.cubeTalk.common.util.RandomNicknameGenerator;
import server.cubeTalk.member.model.entity.Member;
import server.cubeTalk.member.repository.MemberRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final MessageRepository messageRepository;
    private final WebSocketService webSocketService;
    private final SubscriptionManager subscriptionManager;
    private final SimpMessageSendingOperations messageSendingOperations;
    private boolean isRollBack = false;

    /* 채팅방 생성 */
    public ChatRoomCreateResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto) {

        String channelId = UUID.randomUUID().toString();
        String memberId = UUID.randomUUID().toString();
        double totalChatDuration = 0.0;
        if ("찬반".equals(requestDto.getChatMode()) && requestDto.getDebateSettings().isPresent()) {
            DebateSettingsRequest debateSettingsDto = requestDto.getDebateSettings().get();
            totalChatDuration = debateSettingsDto.getNegativeEntry() + debateSettingsDto.getPositiveEntry() + debateSettingsDto.getNegativeRebuttal() + debateSettingsDto.getPositiveRebuttal()
                    + debateSettingsDto.getNegativeQuestioning() + debateSettingsDto.getPositiveQuestioning() + 0.5;
        }
        ChatRoom chatRoom = ChatRoom.builder()
                .channelId(channelId)
                .ownerId(memberId)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .maxParticipants(requestDto.getMaxParticipants())
                .chatMode(requestDto.getChatMode())
                .chatDuration(requestDto.getChatDuration().isPresent() && requestDto.getChatMode().equals("자유") ? requestDto.getChatDuration().get() : totalChatDuration)
                .debateSettings(buildDebateSettings(requestDto))
                .chatStatus("CREATED")
                .build();

        Member member = Member.builder()
                .memberId(memberId)
                .build();

        chatRoomRepository.save(chatRoom);
        memberRepository.save(member);

        return new ChatRoomCreateResponseDto(chatRoom.getId(), memberId);
    }

    /* DebateSettings 빌드 메서드 */
    private DebateSettings buildDebateSettings(ChatRoomCreateRequestDto requestDto) {
        if ("찬반".equals(requestDto.getChatMode()) && requestDto.getDebateSettings().isPresent()) {
            DebateSettingsRequest debateSettingsDto = requestDto.getDebateSettings().get();

            // DebateSettings 빌드
            return DebateSettings.builder()
                    .positiveEntry(debateSettingsDto.getPositiveEntry())
                    .negativeEntry(debateSettingsDto.getNegativeEntry())
                    .positiveRebuttal(debateSettingsDto.getPositiveRebuttal())
                    .negativeRebuttal(debateSettingsDto.getNegativeRebuttal())
                    .positiveQuestioning(debateSettingsDto.getPositiveQuestioning())
                    .negativeQuestioning(debateSettingsDto.getNegativeQuestioning())
                    .votingTime(0.5)
                    .build();
        } else {
            return null;
        }
    }


    /* 채팅방 참가 */
    public ChatRoomJoinResponseDto joinChatRoom(String id, ChatRoomJoinRequestDto chatRoomJoinRequestDto) {
        if (isRollBack) {
            throw new IllegalArgumentException("롤백처리 중이기 때문에 지금은 참가할 수 없습니다.");
        }

        String subchannelId = null;
        String memberId = UUID.randomUUID().toString();

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));

        // 최대 참가 인원 , 팀별 참가 인원에 대한 예외처리
        participantsValidate(chatRoom, chatRoomJoinRequestDto);

        // 새 참가자를 리스트에 추가
        // !chatRoom.getOwnerId().isEmpty() (비어있지않다면) -> 방장이 참가
        // 이후 dto.getOwnerId()랑 chatRoom.getOwnerId() 비교 후 같으면 원래 chatRoom.getOwnerId() 아니면, 예외(유효성검증실패) 처리
        String enterMember;

        if (chatRoomJoinRequestDto.getOwnerId().isPresent()) {
            if (chatRoom.getOwnerId() != null && chatRoom.getOwnerId().equals(chatRoomJoinRequestDto.getOwnerId().get())) {
                enterMember = chatRoom.getOwnerId();
            } else {
                throw new IllegalArgumentException("유효하지 않은 (ownerId) 방장 정보입니다.");
            }

        } else {
            enterMember = memberId;
        }

        String nickName;
        if (chatRoomJoinRequestDto.getNickName() == null) {
            nickName = RandomNicknameGenerator.generateNickname();
        } else {
            if (validateNickName(id, chatRoomJoinRequestDto.getNickName())) {
                nickName = chatRoomJoinRequestDto.getNickName();
            } else {
                throw new IllegalArgumentException("이미 사용중인 닉네임 입니다.");
            }
        }

        Member member = Member.builder()
                .memberId(enterMember)
                .nickName(nickName)
                .build();

        Participant participant = Participant.builder()
                .memberId(enterMember)
                .role(chatRoomJoinRequestDto.getRole())
                .status(chatRoom.getOwnerId().equals(enterMember) ? "OWNER" : "PENDING")
                .nickName(nickName)
                .build();

        chatRoom.getParticipants().add(participant);

        // subChatRoom 생성 및 해당 메인 채팅방에  subChatRoom 이 "subChannelId 가 이미 있고,
        //      "type" = dto.getRole 이랑 같다면 , 해당 방에 subChatRoom.addParticipant 해주기
        // "subChannelId 가 이미 있는데, type이 같은 게 없다면 "subChannelId생성,
        // subChannelId 가 없다면 "subChannelId  생성 및 "type = dto.getRole


        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(chatRoomJoinRequestDto.getRole())) {
                subchannelId = subChatRoom.getSubChannelId();
                subChatRoom.addParticipant(participant);
                break;
            }
        }

        if (subchannelId == null) {
            subchannelId = UUID.randomUUID().toString();
            SubChatRoom newSubChatRoom = SubChatRoom.builder()
                    .subChannelId(subchannelId)
                    .type(chatRoomJoinRequestDto.getRole())
                    .participants(new ArrayList<>()) // 빈 리스트로 초기화
                    .build();

            newSubChatRoom.addParticipant(participant);
            chatRoom.getSubChatRooms().add(newSubChatRoom);
        }

        String message = nickName + "님이 입장하셨습니다.";
//        ChatRoomCommonMessageResponseDto chatMessage = new ChatRoomCommonMessageResponseDto("ENTER", message);
        messageSendingOperations.convertAndSend( "/topic/chat." + chatRoom.getChannelId(), message);
        messageSendingOperations.convertAndSend( "/topic/chat." + subchannelId, message);

        memberRepository.save(member);
        chatRoomRepository.save(chatRoom);

        webSocketService.sendParticiPantsList(chatRoom);

        return new ChatRoomJoinResponseDto(chatRoom.getId(), enterMember, chatRoom.getChannelId(), subchannelId, nickName);
    }


    /* 중복 닉네임 검증 */
    public boolean validateNickName(String roomId, String nickName) {
        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방이 존재하지 않습니다."));

        for (Participant participant : chatRoom.getParticipants()) {
//            if (participant.getNickName() == null) {
//
            if (participant.getNickName().equals(nickName)) {
                return false; // 존재하면 false 반환
            }
        }
        return true;
    }


    /* 팀 변경 */
    public ChatRoomTeamChangeResponseDto changeTeam(String id, String memberId, ChatRoomTeamChangeRequestDto chatRoomTeamChangeRequestDto) {
        if (isRollBack) {
            throw new IllegalArgumentException("현재 롤백 처리 중이기 때문에 변경이 불가능합니다.");
        }

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));

        if (chatRoom.getChatStatus().equals("STARTED"))
            throw new IllegalArgumentException("이미 시작된 채팅방이기 때문에 변경할 수 없습니다.");

        Participant searchParticipant = chatRoom.getParticipants().stream()
                .filter(p -> p.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

        if (searchParticipant.getRole().equals(chatRoomTeamChangeRequestDto.getRole()))
            throw new IllegalArgumentException("이미 해당팀에 속해있습니다.");
        /* 변경하려는 팀의 인원의 가득 찬 경우 */
        String role = chatRoomTeamChangeRequestDto.getRole();

        if (!role.equals("찬성") && !role.equals("반대") && !role.equals("관전")) {
            String errorMessage = "내용에 '찬성 or 반대 or 관전'이 포함되어야 합니다.";
            throw new IllegalArgumentException(errorMessage);
        }

        long roleCount = chatRoom.getParticipants().stream()
                .filter(participant -> role.equals(participant.getRole()))
                .count();

        if (role.equals("찬성") || role.equals("반대")) {
            if (roleCount >= chatRoom.getMaxParticipants() / 2) {
                String errorMessage = "해당 팀의 인원이 꽉 찼기 때문에 변경할 수 없습니다.";
                throw new IllegalArgumentException(errorMessage);
            }
        } else if (role.equals("관전")) {
            if (roleCount >= 4) {
                String errorMessage = "해당 팀의 인원이 꽉 찼기 때문에 변경할 수 없습니다.";
                throw new IllegalArgumentException(errorMessage);
            }
        }

        /* 참가자의 역할 변경 작업 */
        List<Participant> changeParticipant = chatRoom.getParticipants().stream().map(
                participant -> {
                    if (participant.getMemberId().equals(memberId)) {
                        return Participant.builder()
                                .memberId(memberId)
                                .role(chatRoomTeamChangeRequestDto.getRole())
                                .status(participant.getStatus())
                                .nickName(participant.getNickName())
                                .build();
                    }
                    return participant;
                }
        ).toList();

        String originRole = chatRoom.getParticipants().stream()
                .filter(participant -> participant.getMemberId().equals(memberId))
                .map(Participant::getRole)
                .findFirst()
                .orElseThrow(() -> {
                    String errorMessage = "해당 멤버 ID를 찾을 수 없습니다.";
                    return new IllegalArgumentException(errorMessage);
                });

        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                .participants(changeParticipant)
                .build();

        boolean isSubChannelIdFound = false;
        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getSubChannelId().equals(chatRoomTeamChangeRequestDto.getSubChannelId()) && subChatRoom.getType().equals(originRole)) {
                isSubChannelIdFound = true;
                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(memberId));
                break;
            }
        }

        if (!isSubChannelIdFound) {
            String errorMessage = "현재 변경하고자 하는 역할과, 변경 전 subChannel 요청이 달라 처리할 수 없습니다. request 를 확인해주세요. ";
            throw new IllegalArgumentException(errorMessage);
        }

        String[] changeSubChannelId = new String[1];

        chatRoom.getSubChatRooms().forEach(subChatRoom -> {
            if (subChatRoom.getType().equals(chatRoomTeamChangeRequestDto.getRole())) {
                changeParticipant.forEach(participant -> {
                    if (participant.getMemberId().equals(memberId)) {
                        changeSubChannelId[0] = subChatRoom.getSubChannelId();
                        subChatRoom.getParticipants().add(participant);
                    }
                });
            }
        });

        if (changeSubChannelId[0] == null) {
            changeSubChannelId[0] = UUID.randomUUID().toString();
            SubChatRoom newSubChatRoom = SubChatRoom.builder()
                    .subChannelId(changeSubChannelId[0])
                    .type(chatRoomTeamChangeRequestDto.getRole())
                    .participants(new ArrayList<>())
                    .build();
            changeParticipant.forEach(participant -> {
                if (participant.getMemberId().equals(memberId)) {
                    newSubChatRoom.getParticipants().add(participant);
                }
            });
            chatRoom.getSubChatRooms().add(newSubChatRoom);
        }

        chatRoomRepository.save(updatedChatRoom);

        webSocketService.sendParticiPantsList(updatedChatRoom);

        return new ChatRoomTeamChangeResponseDto(id, chatRoom.getChannelId(), changeSubChannelId[0], chatRoomTeamChangeRequestDto.getSubChannelId());
    }


    /* 참가 인원 검증 */
    public void participantsValidate(ChatRoom chatRoom, ChatRoomJoinRequestDto dto) {

        final String SUPPORT_ROLE = "찬성";
        final String OPPOSITE_ROLE = "반대";
        final String SPECTATOR_ROLE = "관전";
        final int MAX_SPECTATORS = 4;

        int currParticipantsCnt = chatRoom.getParticipants().toArray().length;

        if (chatRoom.getMaxParticipants() + MAX_SPECTATORS <= currParticipantsCnt) {
            throw new IllegalArgumentException("현재 참가 인원이 꽉 찼습니다.");
        }

        if (chatRoom.getChatStatus().equals("STARTED") && (dto.getRole().equals(SUPPORT_ROLE) || dto.getRole().equals(OPPOSITE_ROLE))) {
            throw new IllegalArgumentException("채팅이 이미 시작되어 해당 팀으로 입장이 불가능합니다.");
        }

        long supportCount = chatRoom.getParticipants().stream()
                .filter(participant -> SUPPORT_ROLE.equals(participant.getRole()))
                .count();

        long oppsiteCount = chatRoom.getParticipants().stream()
                .filter(participant -> OPPOSITE_ROLE.equals(participant.getRole()))
                .count();

        long spectatorCount = chatRoom.getParticipants().stream()
                .filter(participant -> SPECTATOR_ROLE.equals(participant.getRole()))
                .count();

        if (chatRoom.getChatStatus().equals("STARTED") && dto.getRole().equals(SPECTATOR_ROLE) && spectatorCount >= MAX_SPECTATORS) {
            throw new IllegalArgumentException("현재 관전 인원이 꽉 찼습니다.");
        }

        if ((supportCount != 0 || oppsiteCount != 0 || spectatorCount != 0) && chatRoom.getMaxParticipants() != 0) {
            if (supportCount >= (chatRoom.getMaxParticipants() / 2) && dto.getRole().equals(SUPPORT_ROLE)) {
                throw new IllegalArgumentException("현재 찬성 인원이 꽉 찼습니다.");
            }
            if (oppsiteCount >= (chatRoom.getMaxParticipants() / 2) && dto.getRole().equals(OPPOSITE_ROLE)) {
                throw new IllegalArgumentException("현재 반대 인원이 꽉 찼습니다.");
            }
            if (spectatorCount >= MAX_SPECTATORS && dto.getRole().equals(SPECTATOR_ROLE)) {
                throw new IllegalArgumentException("현재 관전 인원이 꽉 찼습니다.");
            }
        }

    }


    /* 구독 실패시 롤백 처리 */
    @Transactional
    public String subscriptionFailed(String id, ChatRoomSubscriptionFailureDto ChatRoomSubscriptionFailureDto) {
        final String ENTER_FALIED = "참가실패";
        final String TEAM_CHANGE_FALIED = "변경실패";

        String memberId = ChatRoomSubscriptionFailureDto.getMemberId();
        String originRole = ChatRoomSubscriptionFailureDto.getOriginRole();

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방이 존재하지 않습니다."));

        /* 유효성 검증 */
        boolean exists = chatRoom.getParticipants().stream()
                .anyMatch(participant -> participant.getMemberId().equals(memberId));

        if (!exists)
            throw new IllegalArgumentException("해당 memberId가 참가자 목록에 없습니다.");

        try {
            Participant participant = chatRoom.getParticipants().stream()
                    .filter(p -> p.getMemberId().equals(memberId))  // memberId로 참가자 필터링
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("롤백할 해당 참가자가 없습니다. 서버코드 확인"));
            /* 참가 실패시 롤백 처리 */
            if (ChatRoomSubscriptionFailureDto.getType().equals(ENTER_FALIED)) {
                validateRole(participant.getRole(), originRole, "참가자의 역할이 변경하려던 역할과 일치하지 않습니다.");
                rollbackJoin(chatRoom, memberId, originRole);
                isRollBack = true;
            } else if (ChatRoomSubscriptionFailureDto.getType().equals(TEAM_CHANGE_FALIED)) {   /* 팀 변경 실패시 롤백 처리 */
                String newRole = ChatRoomSubscriptionFailureDto.getNewRole()
                        .orElseThrow(() -> new IllegalArgumentException("newRole 필드가 존재하지 않습니다."));
                validateRole(participant.getRole(), newRole, "참가자의 역할이 변경하려던 역할과 일치하지 않습니다.");
                rollbackTeamChange(chatRoom, memberId, originRole, newRole);
                isRollBack = true;
            }

            /* 롤백 완료 메시지 반환 */
            isRollBack = false;
            return "롤백완료";

        } catch (Exception e) {
            throw new RuntimeException("롤백실패 " + e.getMessage());
        }

    }

    private void validateRole(String actualRole, String expectedRole, String errorMessage) {
        if (!actualRole.equals(expectedRole)) {
            throw new IllegalArgumentException(errorMessage);
        }
    }

    /* 참가 실패 롤백 처리 */
    @Transactional
    public void rollbackJoin(ChatRoom chatRoom, String memberId, String originRole) {
        memberRepository.deleteByMemberId(memberId);
        chatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(memberId));
        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(originRole)) {
                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(memberId));
            }
        }
        // 멤버리포지토리도 삭제한 걸 반영해야함
        chatRoomRepository.save(chatRoom);
    }

    /* 팀 변경 실패 롤백 처리 */
    @Transactional
    public void rollbackTeamChange(ChatRoom chatRoom, String memberId, String originRole, String newRole) {

        Participant infoParticipant = chatRoom.getParticipants().stream()
                .filter(participant -> participant.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 memberId의 참가자를 찾을 수 없습니다."));

        List<Participant> participants = chatRoom.getParticipants();

        for (int i = 0; i < participants.size(); i++) {
            Participant participant = participants.get(i);
            if (participant.getMemberId().equals(memberId)) {
                // role과 status가 변경된 새로운 Participant 객체로 교체
                Participant updatedParticipant = Participant.changeRole(participant, originRole, participant.getStatus(), participant.getNickName());
                participants.set(i, updatedParticipant);  // 기존 인덱스에 새 객체로 교체
            }
        }

        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(newRole)) {
                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(memberId));
            }

            if (subChatRoom.getType().equals(originRole)) {
                Participant originParticipant = Participant.builder()
                        .memberId(memberId)
                        .role(originRole)
                        .status(infoParticipant.getStatus())
                        .build();
                subChatRoom.getParticipants().add(originParticipant);
            }
        }

        chatRoomRepository.save(chatRoom);
    }


    /* 토론 개요 수정 */
    public String modifyDescription(String id, ChatRoomModifyDescriptionRequestDto chatRoomModifyDescriptionRequestDto) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        String ownerId = chatRoomModifyDescriptionRequestDto.getOwnerId();
        String modifyDescription = chatRoomModifyDescriptionRequestDto.getDescription();

        if (!chatRoom.getOwnerId().equals(ownerId))
            throw new IllegalArgumentException("방장이 아닙니다. 방장만 채팅방 설명을 수정할 수 있습니다.");

        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                .description(modifyDescription)
                .build();

        chatRoomRepository.save(updatedChatRoom);

        return "요청처리에 성공했습니다.";
    }

    /* home 버튼 get 요청 */
    public ChatRoomHomeResponseDto getDescription(String id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));
        return ChatRoomHomeResponseDto.fromChatRoom(chatRoom);
    }

    /* 참여자 인원 수 */
    public ChatRoomParticipantsCountDto getParticipantCounts(String id) {
        final String SUPPORT = "찬성";
        final String OPPOSITE = "반대";
        final String SPECTATOR = "관전";

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        int supportCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(SUPPORT))
                .count();

        int oppositeCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(OPPOSITE))
                .count();

        int spectatorCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(SPECTATOR))
                .count();

        return new ChatRoomParticipantsCountDto(supportCount+oppositeCount, chatRoom.getMaxParticipants(), supportCount, oppositeCount, spectatorCount);
    }

    /* 채팅방 정보 */
    public ChatRoomInfoResponseDto getChatRoomInfo(String id) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        return ChatRoomInfoResponseDto.fromChatRoom(chatRoom);
    }

    /* 채팅방 설정 변경 */
    public String changeChatRoomSettings(String id, ChatRoomChangeSettingsRequestDto chatRoomChangeSettingsRequestDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));
        if (chatRoom.getChatStatus().equals("STARTED"))
            throw new IllegalArgumentException("이미 시작된 채팅방이기 때문에 변경할 수 없습니다.");
        if (!chatRoom.getOwnerId().equals(chatRoomChangeSettingsRequestDto.getOwnerId())) {
            throw new IllegalArgumentException("채팅 설정은 방장만 변경이 가능합니다.");
        }

        // 찬반 모드일 때 참가자 수 검증
        if ("찬반".equals(chatRoom.getChatMode())) {
            int maxParticipants = chatRoomChangeSettingsRequestDto.getMaxParticipants();
            if (maxParticipants % 2 != 0 || (0 < maxParticipants && maxParticipants < 6)) {
                throw new IllegalArgumentException("찬반토론인 경우, 참가자는 짝수이면서 최소 6명이상이어야 합니다.");
            }

            if (chatRoomChangeSettingsRequestDto.getDebateSettings().isEmpty()) {
                throw new IllegalArgumentException("찬반 토론에서는 토론 설정이 필수입니다.");
            }

            DebateSettingsRequest debateSettings = chatRoomChangeSettingsRequestDto.getDebateSettings().get();
            double totalChatDuration = debateSettings.getNegativeEntry() + debateSettings.getPositiveEntry()
                    + debateSettings.getNegativeQuestioning() + debateSettings.getPositiveQuestioning()
                    + debateSettings.getNegativeRebuttal() + debateSettings.getPositiveRebuttal();

            // 채팅방의 토론 설정과 시간 업데이트
            ChatRoom updatedChatRoom = chatRoom.toBuilder()
                    .maxParticipants(maxParticipants)
                    .chatDuration(totalChatDuration)
                    .debateSettings(debateSettings.toEntity())  // DebateSettingsRequest -> DebateSettings 변환
                    .build();
            chatRoomRepository.save(updatedChatRoom);

        } else {
            // 자유 모드일 때
            if (chatRoomChangeSettingsRequestDto.getChatDuration().isEmpty() || chatRoomChangeSettingsRequestDto.getMaxParticipants() < 0) {
                throw new IllegalArgumentException("자유 모드에서는 채팅 시간, 사용자 수(0명이상)(이)가 필수입니다.");
            }


            ChatRoom updatedChatRoom = chatRoom.toBuilder()
                    .maxParticipants(chatRoomChangeSettingsRequestDto.getMaxParticipants())
                    .chatDuration(chatRoomChangeSettingsRequestDto.getChatDuration().get())
                    .build();
            chatRoomRepository.save(updatedChatRoom);
        }

        return "요청처리에 성공했습니다";
    }

    public ChatRoomBeforeMessagesResponseDto getBeforeMessages(String channelId) {
        List<Message> messages = messageRepository.findByChannelId(channelId);

        List<Message> filteredMessages = messages.stream()
                .filter(message -> message.getCreatedAt().isBefore(DateTimeUtils.nowFromZone())) // createdAt이 get요청 시각보다 이전인 메시지만
                .toList();

        return new ChatRoomBeforeMessagesResponseDto(ChatRoomMessages.fromMessagesByChannelId(filteredMessages, channelId));
    }

    /* 준비상태 변경에 따른 참여자 목록 */
    @Transactional
    public List<ChatRoomParticipantsListResponseDto> sendParticipantsList(String id, ChatRoomReadyStatusRequestDto chatRoomReadyStatusRequestDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));
        final String title = "참여자목록 불러오기";
        if (!chatRoom.getChatMode().equals(chatRoomReadyStatusRequestDto.getType()) || !chatRoomReadyStatusRequestDto.getType().equals("찬반")) {
            webSocketService.sendErrorMessage(title, "채팅방 모드와 request의 type이 일치하지않습니다.");
            throw new IllegalArgumentException("채팅방 모드와 request의 type이 일치하지않습니다.");
        }
        if (chatRoom.getOwnerId().equals(chatRoomReadyStatusRequestDto.getMemberId())) {
            webSocketService.sendErrorMessage(title, "방장은 준비할 수 없습니다.");
            throw new IllegalArgumentException("방장은 준비할 수 없습니다.");
        }
        if (chatRoom.getChatStatus().equals("STARTED")) {
            webSocketService.sendErrorMessage(title, "이미 시작한 채팅방은 준비상태를 변경할 수 없습니다.");
            throw new IllegalArgumentException("이미 시작한 채팅방은 준비상태를 변경할 수 없습니다.");
        }

        Participant p = chatRoom.getParticipants().stream()
                .filter(participant -> participant.getMemberId().equals(chatRoomReadyStatusRequestDto.getMemberId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 member가 없습니다."));


        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(p.getRole())) {
                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(p.getMemberId()));
                break;
            }
        }

        Participant updatedParticipant = Participant.builder()
                .memberId(p.getMemberId())
                .role(p.getRole())
                .status(chatRoomReadyStatusRequestDto.getStatus())
                .nickName(p.getNickName())
                .build();

        List<Participant> updatedParticipants = chatRoom.getParticipants().stream()
                .map(participant -> participant.getMemberId().equals(p.getMemberId()) ? updatedParticipant : participant)
                .toList();

        ChatRoom updateChatRoom = chatRoom.toBuilder()
                .participants(updatedParticipants)
                .build();

        for (SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(updatedParticipant.getRole())) {
                subChatRoom.addParticipant(updatedParticipant);
                break;
            }
        }

        chatRoomRepository.save(updateChatRoom);

        return chatRoom.getParticipants().stream()
                .map(participant -> new ChatRoomParticipantsListResponseDto(
                        participant.getNickName(),
                        participant.getRole(),
                        participant.getStatus()
                ))
                .collect(Collectors.toList());

    }

    @Transactional
    public ChatRoomSendMessageResponseDto sendChatMessage(String channelId, ChatRoomSendMessageRequestDto chatRoomSendMessageRequestDto) {
        ChatRoom chatRoom = chatRoomRepository.findById(chatRoomSendMessageRequestDto.getId())
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방이 존재하지 않습니다."));

//        final String main = "MAIN";

        // 메인 채널 검증
//        if (chatRoomSendMessageRequestDto.getType().equals(main)) {
//            if (!chatRoom.getChannelId().equals(channelId)) {
//                webSocketService.sendErrorMessage("채팅 메시지 예외", "메인 채팅방이 아닙니다. channelId와 type을 다시 확인해주세요.");
//                throw new IllegalArgumentException("메인 채팅방이 아닙니다.");
//            }
//        } else {
//            // 서브 채널 검증
//            validateSubChannel(chatRoom, channelId, chatRoomSendMessageRequestDto.getType());
//        }
//        validateSubChannel(chatRoom, channelId, chatRoomSendMessageRequestDto.getType());
        if (!chatRoom.getChannelId().equals(channelId)) {
            validateSubChannel(chatRoom, channelId, chatRoomSendMessageRequestDto.getType());
        }

        List<Participant> participants = chatRoom.getParticipants();
        if (participants == null || participants.isEmpty()) {
            webSocketService.sendErrorMessage("채팅 메시지 예외", "참가자 목록이 없습니다.");
            return null;
        }

        boolean isParticipantInRoom = participants.stream()
                .anyMatch(participant ->
                        participant.getNickName() != null && participant.getNickName().equals(chatRoomSendMessageRequestDto.getSender())
                );

        // 참가자가 없는 경우
        if (!isParticipantInRoom) {
            webSocketService.sendErrorMessage("채팅 메시지 예외", "해당 채팅방에 없는 멤버입니다.");
            throw new IllegalArgumentException("해당 채팅방에 없는 멤버입니다.");
        }

        Message message = Message.builder()
                .type(chatRoomSendMessageRequestDto.getType())
                .sender(chatRoomSendMessageRequestDto.getSender())
                .channelId(channelId)
                .message(chatRoomSendMessageRequestDto.getMessage())
                .replyToMessageId(chatRoomSendMessageRequestDto.getReplyToMessageId().orElse(null))
                .build();

        messageRepository.save(message);

        return new ChatRoomSendMessageResponseDto(message.getId(), message.getType(), message.getSender(), message.getMessage().toString(), message.getReplyToMessageId(), message.getCreatedAt());
    }

    /* subchannel 유효성 검증 */
    public void validateSubChannel(ChatRoom chatRoom, String channelId, String dtoType) {
        Optional<SubChatRoom> subChannel = chatRoom.getSubChatRooms().stream()
                .filter(subChatRoom -> subChatRoom.getSubChannelId().equals(channelId) && subChatRoom.getType().equals(dtoType))
                .findAny();

        if (!subChannel.isPresent()) {
            webSocketService.sendErrorMessage("채팅 메시지 예외", dtoType + " 채팅방이 아닙니다. channelId와 type을 다시 확인해주세요.");
            throw new IllegalArgumentException(dtoType + " 채팅방이 존재하지 않습니다.");
        }
    }

    /* 채팅방 시작 */
    @Transactional
    public String startChat(String id, ChatRoomStartRequestDto chatRoomStartRequestDto) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방이 존재하지 않습니다."));

        if (!chatRoom.getOwnerId().equals(chatRoomStartRequestDto.getOwnerId()))
            throw new IllegalArgumentException("방장만 시작할 수 있습니다.");

        boolean isPendingParticipant = chatRoom.getParticipants().stream().anyMatch(participant -> participant.getStatus().equals("PENDING"));

        if (isPendingParticipant) {
            throw new IllegalArgumentException("참가자 모두 준비상태여야 시작할 수 있습니다.");
        }

        if (chatRoom.getChatMode().equals("찬반")) {
            long support = chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals("찬성")).count();
            long opposite = chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals("반대")).count();

            if (support < 1 || opposite < 1) throw new IllegalArgumentException("찬성, 반대 측이 최소 1명이상이어야 시작할 수 있습니다.");
        }

        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                .chatStatus("STARTED")
                .build();

        chatRoomRepository.save(updatedChatRoom);

        List<String> participantNickNames = updatedChatRoom.getParticipants()
                .stream()
                .map(Participant::getNickName)
                .collect(Collectors.toList());

        boolean isParticipant = subscriptionManager.isNickNameInList(participantNickNames);

        if (isParticipant) {
            webSocketService.progressChatRoom(updatedChatRoom);
        } else {
            throw new IllegalArgumentException("채팅방에 참가중이지 않기 때문에 채팅방 진행이 어렵습니다.(채팅방 구독 실패)");
        }

        return "채팅방 시작 완료";
    }

    @Transactional
    public void voteChat(String id, ChatRoomVoteRequestDto chatRoomVoteRequestDto) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방이 존재하지 않습니다."));

        if (!chatRoomVoteRequestDto.getType().equals("VOTE")) {
            webSocketService.sendErrorMessage("투표", "type 형식이 잘못되었습니다.");
            throw new IllegalArgumentException("type 형식이 잘못되었습니다.");
        }
        if (!(chatRoomVoteRequestDto.getTeam().equals("SUPPORT") || chatRoomVoteRequestDto.getTeam().equals("OPPOSITE"))) {
            webSocketService.sendErrorMessage("투표", "team 형식이 잘못되었습니다.");
            throw new IllegalArgumentException("team 형식이 잘못되었습니다.");
        }
        boolean isMVP = chatRoom.getParticipants().stream().anyMatch(participant -> participant.getNickName().equals(chatRoomVoteRequestDto.getMvp()));
        if (!isMVP) {
            webSocketService.sendErrorMessage("투표", "해당 닉네임을 가진 참가자가 없습니다.");
            throw new IllegalArgumentException("해당 닉네임을 가진 참가자가 없습니다.");
        }

        int support = 0;
        int opposite = 0;

        ChatRoom updateChatRoom = chatRoom.toBuilder()
                .vote(buildVote(chatRoomVoteRequestDto, support,opposite,chatRoom))
                .build();

        chatRoomRepository.save(updateChatRoom);

    }

    @Transactional
    private Vote buildVote(ChatRoomVoteRequestDto chatRoomVoteRequestDto, int support, int opposite,ChatRoom chatRoom) {

        Vote vote = chatRoom.getVote() != null ? chatRoom.getVote() : new Vote(0, 0, new ArrayList<>());

        if (chatRoomVoteRequestDto.getTeam().equals("SUPPORT")) {
            support++;
        } else {
            opposite++;
        }
        return vote.addMVP(chatRoomVoteRequestDto.getMvp())
                .toBuilder()
                .support(support)
                .opposite(opposite)
                .build();

    }

    /* 채팅방 목록 페이지 네이션 */
    public List<ChatRoomFilterListResponseDto> getFilteredChatRooms(String mode, String sort, String order, String status, int page, int size) {
        Sort.Direction direction = order.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort sortCriteria;

        // 정렬 기준에 따른 Sort 객체 생성
        if (sort.equalsIgnoreCase("participants")) {
            sortCriteria = Sort.by(direction, "maxParticipants"); // 참가자 수 기준으로 정렬
        } else {
            sortCriteria = Sort.by(direction, "createdAt"); // 기본적으로 생성일 기준으로 정렬
        }

        Pageable pageable = PageRequest.of(page, size, sortCriteria);

        // 상태별로 필터링된 채팅방 목록 가져오기
        Page<ChatRoom> chatRooms;

        // mode가 null 또는 빈 문자열일 경우 모든 채팅방 필터링
        if (mode == null || mode.isEmpty()) {
            if (status.equals("STARTED")) {
                chatRooms = chatRoomRepository.findByChatStatus("STARTED", pageable);
            } else if (status.equals("CREATED")) {
                chatRooms = chatRoomRepository.findByChatStatus("CREATED", pageable);
            } else {
                chatRooms = chatRoomRepository.findAll(pageable); // 모든 채팅방
            }
        } else {
            if (status.equals("STARTED")) {
                chatRooms = chatRoomRepository.findByChatModeAndChatStatus(mode, "STARTED", pageable);
            } else if (status.equals("CREATED")) {
                chatRooms = chatRoomRepository.findByChatModeAndChatStatus(mode, "CREATED", pageable);
            } else {
                chatRooms = chatRoomRepository.findByChatMode(mode, pageable);
            }
        }

        // 페이지 결과를 DTO로 변환
        return chatRooms.getContent().stream()
                .map(this::toChatRoomFilterListResponseDto)
                .collect(Collectors.toList());
    }


    private ChatRoomFilterListResponseDto toChatRoomFilterListResponseDto(ChatRoom chatRoom) {
        String ownerNickName = chatRoom.getParticipants().stream()
                .filter(p -> p.getStatus().equals("OWNER"))
                .findFirst()
                .flatMap(p -> Optional.ofNullable(p.getNickName())) // null일 수 있는 nickName 처리
                .orElse("Unknown");

        int supportCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals("찬성"))
                .count();
        int oppositeCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals("반대"))
                .count();

        int currentParticipantsCount = supportCount + oppositeCount;

        return new ChatRoomFilterListResponseDto(
                chatRoom.getId(),
                chatRoom.getChatMode(),
                chatRoom.getTitle(),
                chatRoom.getDescription(),
                chatRoom.getChatDuration(),
                ownerNickName, // 필터링된 소유자의 닉네임 사용
                chatRoom.getMaxParticipants(),
                currentParticipantsCount,
                chatRoom.getCreatedAt(),
                chatRoom.getUpdatedAt()
        );
    }
}






