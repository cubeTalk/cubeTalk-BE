package server.cubeTalk.chat.model.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class ChatRoomVoteRequestDto {
    private String type;
    private Optional<String> team = Optional.empty();
    private String mvp;
}
