package server.cubeTalk.chat.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.cubeTalk.chat.model.entity.DebateSettings;

@Getter
@NoArgsConstructor
public class DebateSettingsRequest {
    private int positiveEntry;
    private int negativeQuestioning;
    private int negativeEntry;
    private int positiveQuestioning;
    private int positiveRebuttal;
    private int negativeRebuttal;

    // DTO를 엔티티로 변환하는 메서드
    public DebateSettings toEntity() {
        return DebateSettings.builder()
                .positiveEntry(this.positiveEntry)
                .negativeEntry(this.negativeEntry)
                .positiveRebuttal(this.positiveRebuttal)
                .negativeRebuttal(this.negativeRebuttal)
                .positiveQuestioning(this.positiveQuestioning)
                .negativeQuestioning(this.negativeQuestioning)
                .votingTime(0.5)
                .build();
    }

}
