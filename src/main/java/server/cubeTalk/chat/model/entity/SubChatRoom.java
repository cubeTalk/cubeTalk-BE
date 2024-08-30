package server.cubeTalk.chat.model.entity;


import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.ArrayList;

@Data
@Builder
public class SubChatRoom {
    private String subChannelId;
    private String type;
    private List<Participant> participants;

    public void addParticipant(Participant participant) {
        this.participants.add(participant);
    }

}
