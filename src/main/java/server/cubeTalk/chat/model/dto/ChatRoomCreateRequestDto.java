package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatRoomCreateRequestDto {
    private String title;
    private String description;
    private int maxParticipants;
    private String nickName;
    private String role;
    private int positiveEntry;
    private int negativeQuestioning;
    private int negativeEntry;
    private int positiveQuestioning;
    private int positiveRebuttal;
    private int negativeRebuttal;
    private int votingTime;
    private int chatDuration;
}
