package server.cubeTalk.chat.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import server.cubeTalk.common.entity.BaseTimeStamp;

@Builder(toBuilder = true)
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Participant extends BaseTimeStamp {
    private String memberId;
    private String role;
    private String status;
    private String nickName;

    public static Participant changeRole(Participant participant, String newRole, String status, String nickName) {
        return new Participant(participant.getMemberId(), newRole, participant.getStatus(), participant.getNickName());  // 새 객체 반환
    }

}
