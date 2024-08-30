package server.cubeTalk.chat.model.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatRoomJoinRequestDto {
    private String nickName;
    private String role;
    private String ownerId;
}
