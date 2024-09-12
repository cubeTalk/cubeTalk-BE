package server.cubeTalk.chat.model.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
@Getter
public class ChatRoomChangeSettingsRequestDto {
    private String ownerId;
    private int maxParticipants; //'찬반'이면 짝수 및 6명이상인지 검증함, Not empty
    private Optional<Double> chatDuration = Optional.empty(); //"찬반"이면 해당 필드 포함 안 하기
    private Optional<DebateSettingsRequest> debateSettings = Optional.empty();  //-> "자유"면 해당 필드 포함 안 하기
}
