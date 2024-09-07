package server.cubeTalk.chat.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomCreateRequestDto {
    @NotEmpty(message = "제목은 필수입니다.")
    @Size(min = 3, max = 50, message = "제목은 3자 이상 50자 이하이어야 합니다.")
    private String title;
    private String description;
//    @Min(value = 6, message = "찬반토론은 최소 6명이상부터 가능합니다.")
    private int maxParticipants;
    @NotEmpty(message = "채팅방 모드는 필수 항목입니다.")
    private String chatMode;
    private int chatDuration;
}
