package server.cubeTalk.chat.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@Getter
@NoArgsConstructor
public class ChatRoomSendMessageRequestDto {
    @NotEmpty(message = "id는 필수 입력값입니다.")
    private String id;
    @NotEmpty(message = "type은 필수 입력값입니다.")
    private String type;
    @NotEmpty(message = "sender 필수 입력값입니다.")
    private String sender;
    private String message;
    private Optional<String> replyToMessageId = Optional.empty();
}
