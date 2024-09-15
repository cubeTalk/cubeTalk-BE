package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomVoteResultResponseDto {
    private int support;
    private int opposite;
    private String mvp;
}
