package server.cubeTalk.chat.model.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ChatRoomJoinResponseDto {

    private String id;
    private String memberId;
    private String channelId;
    private String subChannelId;
    private String nickName;
    private ChatRoomInfoResponseDto chatRoomInfo;
    private LocalDateTime severTimeStamp;

}
