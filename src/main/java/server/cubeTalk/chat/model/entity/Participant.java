package server.cubeTalk.chat.model.entity;


import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Participant {
    private String memberId;
    private String role;
    private String status;
}
