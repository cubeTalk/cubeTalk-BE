package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomCreateRequestDto {
    private String title;
    private String description;
    private int maxParticipants;
    private String chatMode;
    private int chatDuration;
}
