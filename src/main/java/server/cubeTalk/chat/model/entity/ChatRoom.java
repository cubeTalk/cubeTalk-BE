package server.cubeTalk.chat.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.cubeTalk.common.entity.BaseTimeStamp;

import java.util.ArrayList;
import java.util.List;

@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Document(collection = "chatRoom")
public class ChatRoom extends BaseTimeStamp {

    @Id
    private String id;
    private String channelId;
    private String title;
    private String description;
    private String chatMode;
    private int maxParticipants;
    private int chatDuration;
    private String ownerId; //member의 id를 참조
    private String chatStatus;
    private DebateSettings debateSettings;
    private List<Participant> participants = new ArrayList<>(); // 참여자 목록
    private List<SubChatRoom> subChatRooms = new ArrayList<>(); // 서브 채팅방 목록


}

