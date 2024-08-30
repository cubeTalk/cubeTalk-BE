package server.cubeTalk.chat.service;


import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;
import server.cubeTalk.chat.model.dto.ChatRoomCreateResponseDto;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.repository.ChatRoomRepository;
import server.cubeTalk.member.model.entity.Member;
import server.cubeTalk.member.repository.MemberRepository;

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
}
