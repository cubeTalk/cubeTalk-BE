package server.cubeTalk.chat.model.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Getter
public class ChatRoomModifyDescriptionRequestDto {

    @NotEmpty(message = "방장 uuid는 필수입니다.")
    private String ownerId;
    @NotEmpty(message = "개요는 필수입니다.")
    private String description;
}
