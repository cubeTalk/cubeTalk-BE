package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomTeamChangeResponseDto {

    private String id;
    private String ChannelId;
    private String subChannelId;

}
