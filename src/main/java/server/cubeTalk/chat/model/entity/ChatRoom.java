package server.cubeTalk.chat.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.mapping.Document;
import server.cubeTalk.common.entity.BaseTimeStamp;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Document(collection = "chatRoom")
@CompoundIndexes({
        @CompoundIndex(def = "{'id': 1, 'subChatRooms.type': 1}"),
        @CompoundIndex(def = "{'id': 1, 'participants.memberId': 1}")
})
public class ChatRoom extends BaseTimeStamp {

    @Id
    private String id;
    private String channelId;
    private String title;
    private String description;
    private String chatMode;
    private int maxParticipants;
    private Double chatDuration;
    private String ownerId; //member의 id를 참조
    private String chatStatus;
    private DebateSettings debateSettings;
    private Vote vote;
    private List<Participant> participants = new ArrayList<>(); // 참여자 목록
    private List<SubChatRoom> subChatRooms = new ArrayList<>(); // 서브 채팅방 목록


}

