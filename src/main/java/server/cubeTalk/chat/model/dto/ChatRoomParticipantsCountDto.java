package server.cubeTalk.chat.model.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ChatRoomParticipantsCountDto {
    private Integer totalCount;
    private int maxCapacityCount;
    private Integer supportCount;
    private Integer oppositeCount;
    private Integer spectatorCount;
}
