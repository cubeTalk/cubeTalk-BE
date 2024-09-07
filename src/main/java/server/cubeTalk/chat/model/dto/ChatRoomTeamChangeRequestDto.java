package server.cubeTalk.chat.model.dto;


import lombok.Data;


@Data
public class ChatRoomTeamChangeRequestDto {

    private String id;
    private String memberId;
    private String role;
    private String subChannelId;

}
