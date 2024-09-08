package server.cubeTalk.chat.model.dto;


import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import server.cubeTalk.chat.exception.ValidMajor;


@Data
public class ChatRoomTeamChangeRequestDto {

    @NotEmpty(message = "역할은 비워둘 수 없습니다.")
    @ValidMajor(word = {"찬성", "반대", "관전"}, message = "내용에 '찬성 or 반대 or 관전'이 포함되어야 합니다.")
    private String role;
    private String subChannelId;

}
