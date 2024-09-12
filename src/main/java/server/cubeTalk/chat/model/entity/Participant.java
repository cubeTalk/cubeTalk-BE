package server.cubeTalk.chat.model.entity;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Participant {
    private String memberId;
    private String role;
    private String status;
    private String nickName;

    public static Participant changeRole(Participant participant, String newRole, String status, String nickName) {
        return new Participant(participant.getMemberId(), newRole, participant.getStatus(), participant.getNickName());  // 새 객체 반환
    }

}
