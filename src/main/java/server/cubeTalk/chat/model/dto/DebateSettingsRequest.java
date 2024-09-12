package server.cubeTalk.chat.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DebateSettingsRequest {
    private int positiveEntry;
    private int negativeQuestioning;
    private int negativeEntry;
    private int positiveQuestioning;
    private int positiveRebuttal;
    private int negativeRebuttal;

}
