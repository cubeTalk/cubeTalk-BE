package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.*;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.Participant;
import server.cubeTalk.chat.model.entity.SubChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
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

    return new ChatRoomCreateResponseDto(channelId, chatRoom.getId());
    }

    public ChatRoomJoinResponseDto joinChatRoom(String channelId, ChatRoomJoinRequestDto chatRoomJoinRequestDto) {

        String subchannelId = null;
        String memberId = UUID.randomUUID().toString();

        ChatRoom chatRoom = chatRoomRepository.findByChannelId(channelId)
                .orElseThrow(() -> new IllegalArgumentException("해당 채널을 찾을 수 없습니다."));

        // 새 참가자를 리스트에 추가
        // !chatRoom.getOwnerId().isEmpty() (비어있지않다면) -> 방장이 참가
        // 이후 dto.getOwnerId()랑 chatRoom.getOwnerId() 비교 후 같으면 원래 chatRoom.getOwnerId() 아니면, 예외(유효성검증실패) 처리
        String enterMember;

        if (!chatRoomJoinRequestDto.getOwnerId().isEmpty()) {
            if (!chatRoom.getOwnerId().isEmpty() && chatRoom.getOwnerId().equals(chatRoomJoinRequestDto.getOwnerId())) {
                enterMember = chatRoom.getOwnerId();
            } else {
                throw new IllegalArgumentException("유효하지 않은 (ownerId) 방장 정보입니다.");
            }
        } else {
            enterMember = memberId;
        }

        String nickName;
        if (chatRoomJoinRequestDto.getNickName().isEmpty()) {
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

        memberRepository.save(member);

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

        chatRoomRepository.save(chatRoom);

        return new ChatRoomJoinResponseDto(chatRoom.getId(), enterMember, channelId, subchannelId, nickName);
    }

    /* 중복 닉네임 검증 */
    public boolean validateNickName(String nickName) {
        return !memberRepository.existsByNickName(nickName); // 존재하면 false 반환
    }

    /* 팀 변경 */
    public ChatRoomTeamChangeResponseDto changeTeam(String id, String memberId, ChatRoomTeamChangeRequestDto chatRoomTeamChangeRequestDto) {

        ChatRoom chatRoom = chatRoomRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("해당 채팅방을 찾을 수 없습니다."));

        /* 변경하려는 팀의 인원의 가득 찬 경우 */
        String role = chatRoomTeamChangeRequestDto.getRole();

        if (!role.equals("찬성") && !role.equals("반대") && !role.equals("관전")) {
            String errorMessage =  "내용에 '찬성 or 반대 or 관전'이 포함되어야 합니다.";
            webSocketService.sendErrorMessage(chatRoom.getChannelId(),errorMessage);
            throw new IllegalArgumentException(errorMessage);
        }

        long roleCount = chatRoom.getParticipants().stream()
                .filter(participant -> role.equals(participant.getRole()))
                .count();

        if (role.equals("찬성") || role.equals("반대")) {
            if (roleCount >= chatRoom.getMaxParticipants() / 2) {
                String errorMessage = "해당 팀의 인원이 꽉 찼기 때문에 변경할 수 없습니다.";
                webSocketService.sendErrorMessage(chatRoom.getChannelId(),errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }
        } else if (role.equals("관전")) {
            if (roleCount >= 4 ) {
                String errorMessage = "해당 팀의 인원이 꽉 찼기 때문에 변경할 수 없습니다.";
                webSocketService.sendErrorMessage(chatRoom.getChannelId(),errorMessage);
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
                    webSocketService.sendErrorMessage(chatRoom.getChannelId(), errorMessage);
                    return new IllegalArgumentException(errorMessage);
                });

        ChatRoom updatedChatRoom = chatRoom.toBuilder()
                .participants(changeParticipant)
                .build();

        boolean  isSubChannelIdFound = false;
        for (SubChatRoom subChatRoom :chatRoom.getSubChatRooms()) {
            if (subChatRoom.getSubChannelId().equals(chatRoomTeamChangeRequestDto.getSubChannelId()) && subChatRoom.getType().equals(originRole)) {
                isSubChannelIdFound = true;
                subChatRoom.getParticipants().removeIf(participant -> participant.getMemberId().equals(memberId));
                break;
            }
        }

        if (!isSubChannelIdFound) {
            String errorMessage = "현재 변경하고자 하는 역할과, 변경 전 subChannel 요청이 달라 처리할 수 없습니다. request 를 확인해주세요. ";
            webSocketService.sendErrorMessage(chatRoom.getChannelId(), errorMessage);
            throw new IllegalArgumentException(errorMessage);}

        String[] changeSubChannelId = new String[1];

        chatRoom.getSubChatRooms().forEach(subChatRoom -> {
            if (subChatRoom.getType().equals(chatRoomTeamChangeRequestDto.getRole())) {
                changeParticipant.forEach(participant -> {
                    if (participant.getMemberId().equals(memberId)) {
                        changeSubChannelId[0] = subChatRoom.getSubChannelId();
//                        subChatRoom.getParticipants().add(participant);
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

        return new ChatRoomTeamChangeResponseDto(id,chatRoom.getChannelId(), changeSubChannelId[0]);
    }
}
