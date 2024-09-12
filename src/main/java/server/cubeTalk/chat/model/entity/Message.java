package server.cubeTalk.chat.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.cubeTalk.common.entity.BaseTimeStamp;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "message")
public class Message extends BaseTimeStamp {
    @Id
    private String id;
    private String type;
    private String sender;
    private String channelId;
    private Object message;
    private String replyToMessageId;

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void newContent() {
        this.type = "new";
    }

    public void closeContent() {
        this.type = "close";
    }

}
