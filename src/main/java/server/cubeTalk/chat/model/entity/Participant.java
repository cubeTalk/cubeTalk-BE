package server.cubeTalk.chat.model.entity;


import lombok.Builder;

@Builder
public class Participant {
    private String memberId;
    private String role;
    private String status;
}
