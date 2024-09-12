package server.cubeTalk.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.DebateSettings;
import server.cubeTalk.chat.model.entity.Participant;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomInfoResponseDto {
    private String id;
    private String channelId;
    private String titale;
    private String description;
    private String chatMode;
    private int maxParticipants;
    private int chatDuration;
    private String ownerId;
    private String chatStatus;
    private DebateSettings debateSettings;  // 자유 모드가 아닐 때만 포함
    private List<Participant> participants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ChatRoomInfoResponseDto fromChatRoom(ChatRoom chatRoom) {
        return ChatRoomInfoResponseDto.builder()
                .id(chatRoom.getId())
                .channelId(chatRoom.getChannelId())
                .title(chatRoom.getTitle())
                .description(chatRoom.getDescription())
                .chatMode(chatRoom.getChatMode())
                .maxParticipants(chatRoom.getMaxParticipants())
                .chatDuration(chatRoom.getChatDuration())
                .ownerId(chatRoom.getOwnerId())
                .chatStatus(chatRoom.getChatStatus())
                .participants(chatRoom.getParticipants())
                .debateSettings(chatRoom.getChatMode().equals("자유") ? null : chatRoom.getDebateSettings())
                .createdAt(chatRoom.getCreatedAt())
                .updatedAt(chatRoom.getUpdatedAt())
                .build();
    }
}
