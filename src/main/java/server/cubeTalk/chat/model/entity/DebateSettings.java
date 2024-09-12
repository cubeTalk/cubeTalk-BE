package server.cubeTalk.chat.model.entity;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class DebateSettings {
    private int positiveEntry;
    private int negativeEntry;
    private int positiveRebuttal;
    private int negativeRebuttal;
    private int negativeQuestioning;
    private int positiveQuestioning;
    private Double votingTime;

}
