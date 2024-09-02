package server.cubeTalk.chat.model.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Message {
    private String type;
    private String sender;
    private String channelId;
    private Object data;
    private LocalDateTime timestamp;

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
