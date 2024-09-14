package server.cubeTalk.chat.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ChatRoomReadyStatusRequestDto {
    @NotEmpty(message = "type은 비워둘 수 없습니다.")
    private String type;
    @NotEmpty(message = "memberId는 비워둘 수 없습니다.")
    private String memberId;
    @NotEmpty(message = "status는 비워둘 수 없습니다.")
    private String status;
}
