package server.cubeTalk.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomSendMessageResponseDto {

    private String messageId;
    private String type;
    private String sender;
    private String message;
    private String replyToMessageId;
    private LocalDateTime serverTimestamp;



}
