package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.cubeTalk.chat.exception.ValidMajor;

import java.util.Optional;

@Getter
@Data
@NoArgsConstructor
public class ChatRoomSubscriptionFailureDto {
     @ValidMajor(word = {"참가실패", "변경실패"}, message = "내용에 '참가실패 or 변결실패'이 포함되어야 합니다.")
     private String type;
     private String memberId;
     @ValidMajor(word = {"찬성", "반대", "관전"}, message = "내용에 '찬성 or 반대 or 관전'이 포함되어야 합니다.")
     private String originRole;
     private Optional<String> newRole = Optional.empty();
}
