package server.cubeTalk.chat.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomJoinResponseDto {

    private String id;
    private String memberId;
    private String channelId;
    private String subChannelId;
    private String nickName;

}
