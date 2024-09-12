package server.cubeTalk.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import server.cubeTalk.chat.model.entity.ChatRoom;
import server.cubeTalk.chat.model.entity.DebateSettings;

@Getter
@Builder
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomHomeResponseDto {
    private String description;
    private DebateSettings debateSettings;

    public static ChatRoomHomeResponseDto fromChatRoom(ChatRoom chatRoom) {
        return ChatRoomHomeResponseDto.builder()
                .description(chatRoom.getDescription())
                .debateSettings(chatRoom.getChatMode().equals("자유") ? null : chatRoom.getDebateSettings())
                .build();
    }
}
