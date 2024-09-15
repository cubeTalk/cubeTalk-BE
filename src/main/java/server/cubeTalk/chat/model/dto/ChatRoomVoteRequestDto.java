package server.cubeTalk.chat.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomVoteRequestDto {
    private String type;
    private String team;
    private String mvp;
}
