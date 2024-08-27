package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatRoomCreateResponseDto {
    private String  channelID;
    private String id; // 사용자 고유 식별 id (UUID)
}

