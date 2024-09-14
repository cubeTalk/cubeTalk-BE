package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class ChatRoomBeforeMessagesResponseDto {

    private List<ChatRoomMessages> mainChat;

}
