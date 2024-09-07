package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ChatRoomCreateResponseDto {
    private String id; // chatRoom id (UUID)
    private String memberId;
}

