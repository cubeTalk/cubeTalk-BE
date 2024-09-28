package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ChatRoomParticipantsCountDto {
    private int totalCount;
    private int maxCapacityCount;
    private int supportCount;
    private int oppositeCount;
    private int spectatorCount;
}
