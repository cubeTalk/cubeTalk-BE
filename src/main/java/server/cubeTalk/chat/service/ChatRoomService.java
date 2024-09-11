package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import server.cubeTalk.chat.model.dto.*;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.common.dto.CommonResponseDto;
import server.cubeTalk.common.util.DateTimeUtils;
import server.cubeTalk.common.util.RandomNicknameGenerator;
import server.cubeTalk.member.model.entity.Member;
import server.cubeTalk.member.repository.MemberRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;
    private final WebSocketService webSocketService;

    public ChatRoomCreateResponseDto createChatRoom(ChatRoomCreateRequestDto requestDto) {

        String channelId = UUID.randomUUID().toString();
        String memberId = UUID.randomUUID().toString();

        ChatRoom chatRoom = ChatRoom.builder()
                .channelId(channelId)
                .ownerId(memberId)
                .title(requestDto.getTitle())
                .description(requestDto.getDescription())
                .maxParticipants(requestDto.getMaxParticipants())
                .chatMode(requestDto.getChatMode())
                .chatDuration(requestDto.getChatDuration())
                .chatStatus("CREATE")
                .build();

        Member member = Member.builder()
                .memberId(memberId)
                .build();

        chatRoomRepository.save(chatRoom);
        memberRepository.save(member);

    return new ChatRoomCreateResponseDto(chatRoom.getId(),memberId);
    }


    public ChatRoomJoinResponseDto joinChatRoom(String id, ChatRoomJoinRequestDto chatRoomJoinRequestDto) {

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
            }else {
                throw new IllegalArgumentException("유효하지 않은 (ownerId) 방장 정보입니다.");
            }

        } else {
            enterMember = memberId;
        }

        String nickName;
        if (chatRoomJoinRequestDto.getNickName() == null) {
            nickName = RandomNicknameGenerator.generateNickname();
        } else {
            if (validateNickName(chatRoomJoinRequestDto.getNickName())) {
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
                .status("PENDING")
                .build();

        chatRoom.getParticipants().add(participant);

        // subChatRoom 생성 및 해당 메인 채팅방에  subChatRoom 이 "subChannelId 가 이미 있고,
        //      "type" = dto.getRole 이랑 같다면 , 해당 방에 subChatRoom.addParticipant 해주기
        // "subChannelId 가 이미 있는데, type이 같은 게 없다면 "subChannelId생성,
        // subChannelId 가 없다면 "subChannelId  생성 및 "type = dto.getRole


        for ( SubChatRoom subChatRoom : chatRoom.getSubChatRooms() ) {
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

        memberRepository.save(member);
        chatRoomRepository.save(chatRoom);

        return new ChatRoomJoinResponseDto(chatRoom.getId(), enterMember, chatRoom.getChannelId(), subchannelId, nickName, DateTimeUtils.nowFromZone());
    }


    /* 중복 닉네임 검증 */
    public boolean validateNickName(String nickName) {
        return !memberRepository.existsByNickName(nickName); // 존재하면 false 반환
    }


    /* 팀 변경 */
    public ChatRoomTeamChangeResponseDto changeTeam(String id, String memberId, ChatRoomTeamChangeRequestDto chatRoomTeamChangeRequestDto) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));

        Participant searchParticipant = chatRoom.getParticipants().stream()
                .filter(p -> p.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(()->new IllegalArgumentException("해당 멤버를 찾을 수 없습니다."));

        if (searchParticipant.getRole().equals(chatRoomTeamChangeRequestDto.getRole())) throw new IllegalArgumentException("이미 해당팀에 속해있습니다.");
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

        return new ChatRoomTeamChangeResponseDto(id, chatRoom.getChannelId(), changeSubChannelId[0],chatRoomTeamChangeRequestDto.getSubChannelId());
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

        if ( (supportCount != 0 || oppsiteCount != 0 || spectatorCount != 0) && chatRoom.getMaxParticipants() != 0) {
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
    public String subscriptionFailed(String id, ChatRoomSubscriptionFailureDto ChatRoomSubscriptionFailureDto){
        final String ENTER_FALIED = "참가실패";
        final String TEAM_CHANGE_FALIED = "변경실패";

        String memberId = ChatRoomSubscriptionFailureDto.getMemberId();
        String originRole = ChatRoomSubscriptionFailureDto.getOriginRole();

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(()-> new IllegalArgumentException("해당채팅방이 존재하지 않습니다."));

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
            }   else if (ChatRoomSubscriptionFailureDto.getType().equals(TEAM_CHANGE_FALIED)) {   /* 팀 변경 실패시 롤백 처리 */
                String newRole = ChatRoomSubscriptionFailureDto.getNewRole()
                        .orElseThrow(() -> new IllegalArgumentException("newRole 필드가 존재하지 않습니다."));
                validateRole(participant.getRole(), newRole, "참가자의 역할이 변경하려던 역할과 일치하지 않습니다.");
                rollbackTeamChange(chatRoom, memberId, originRole ,newRole);
            }

            /* 롤백 완료 메시지 반환 */
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
    public void rollbackTeamChange(ChatRoom chatRoom, String memberId, String originRole ,String newRole) {

        Participant infoParticipant = chatRoom.getParticipants().stream()
                .filter(participant -> participant.getMemberId().equals(memberId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 memberId의 참가자를 찾을 수 없습니다."));

        List<Participant> participants = chatRoom.getParticipants();

        for (int i = 0; i < participants.size(); i++) {
            Participant participant = participants.get(i);
            if (participant.getMemberId().equals(memberId)) {
                // role과 status가 변경된 새로운 Participant 객체로 교체
                Participant updatedParticipant = Participant.changeRole(participant, originRole, participant.getStatus());
                participants.set(i, updatedParticipant);  // 기존 인덱스에 새 객체로 교체
            }
        }

        for(SubChatRoom subChatRoom : chatRoom.getSubChatRooms()) {
            if (subChatRoom.getType().equals(newRole)){
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
                .orElseThrow(()->new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        String ownerId = chatRoomModifyDescriptionRequestDto.getOwnerId();
        String modifyDescription = chatRoomModifyDescriptionRequestDto.getDescription();

        if (!chatRoom.getOwnerId().equals(ownerId)) throw new IllegalArgumentException("방장이 아닙니다. 방장만 채팅방 설명을 수정할 수 있습니다.");

        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                .description(modifyDescription)
                .build();

        chatRoomRepository.save(updatedChatRoom);

        return "요청처리에 성공했습니다.";
    }

    /* home 버튼 get 요청 */
    public ChatRoomDescriptionResponseDto getDescription(String id) {
        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));
        return new ChatRoomDescriptionResponseDto(chatRoom.getDescription());
    }

    /* 참여자 인원 수 */
    public ChatRoomParticipantsCountDto getParticipantCounts(String id) {
        final String SUPPORT = "찬성";
        final String OPPOSITE = "반대";
        final String SPECTATOR = "관전";

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        int supportCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(SUPPORT))
                .count();

        int oppositeCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(OPPOSITE))
                .count();

        int spectatorCount = (int) chatRoom.getParticipants().stream().filter(participant -> participant.getRole().equals(SPECTATOR))
                .count();

        return new ChatRoomParticipantsCountDto(chatRoom.getMaxParticipants(),chatRoom.getParticipants().size(),supportCount,oppositeCount,spectatorCount);
    }

    /* 채팅방 정보 */
    public ChatRoomInfoResponseDto getChatRoomInfo(String id) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("해당채팅방을 찾을 수 없습니다."));

        return ChatRoomInfoResponseDto.fromChatRoom(chatRoom);
    }

}
