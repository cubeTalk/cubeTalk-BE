package server.cubeTalk.chat.model.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.cubeTalk.chat.exception.ValidMajor;

@Data
@NoArgsConstructor
public class ChatRoomJoinRequestDto {
    private String nickName;
    @NotEmpty(message = "역할은 비워둘 수 없습니다.")
    @ValidMajor(word = {"찬성", "반대", "관전"}, message = "내용에 '찬성 or 반대 or 관전'이 포함되어야 합니다.")
    private String role;
    private String ownerId;
}
