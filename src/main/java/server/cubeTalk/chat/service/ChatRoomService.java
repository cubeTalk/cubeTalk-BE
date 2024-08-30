package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomCreateResponseDto;
import server.cubeTalk.chat.model.dto.ChatRoomJoinRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomJoinResponseDto;
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

@Service
@RequiredArgsConstructor
public class ChatRoomService {

    private final ChatRoomRepository chatRoomRepository;
    private final MemberRepository memberRepository;

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
                .orElseThrow(() -> new RuntimeException("해당 채널을 찾을 수 없습니다."));

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

        String nickName = chatRoomJoinRequestDto.getNickName().isEmpty()
                ? RandomNicknameGenerator.generateNickname()
                : chatRoomJoinRequestDto.getNickName();

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

    public boolean validateNickName(String nickName) {
        if (memberRepository.existsByNickName(nickName).isPresent()) return false;
        return true;
    }
}
