package server.cubeTalk.chat.model.entity;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Participant {
    private String memberId;
    private String role;
    private String status;

    public static Participant changeRole(Participant participant, String newRole, String status) {
        return new Participant(participant.getMemberId(), newRole, participant.getStatus());  // 새 객체 반환
    }

}
