package server.cubeTalk.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import server.cubeTalk.common.dto.CommonResponseDto;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomProgressResponseDto<T> {

    private String type;
    private String remainingTime;
    private String message;
    private T result;

    public record ChatRoomBasicProgressResponse(String type, String remainingTime, String message) {
        public static ChatRoomProgressResponseDto.ChatRoomBasicProgressResponse progress(String type, String remainingTime, String message ) {
            return new ChatRoomProgressResponseDto.ChatRoomBasicProgressResponse(type,remainingTime,message);
        }
    }

}
