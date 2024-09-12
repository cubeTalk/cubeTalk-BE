package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import server.cubeTalk.chat.model.entity.Message;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
@AllArgsConstructor
public class ChatRoomMessages {

    private String messageId;
    private String type;
    private String sender;
    private Object message;
    private String replyToMessageId;
    private LocalDateTime severTimeStamp;

    public static List<ChatRoomMessages> fromMessagesByChannelId(List<Message> messages, String channelId) {
        return messages.stream()
//                .filter(message -> channelId.equals(message.getChannelId())) // 같은 channelId인 메시지 필터링
                .map(message -> ChatRoomMessages.builder()
                        .messageId(message.getId())
                        .type(message.getType())
                        .sender(message.getSender())
                        .message(message.getMessage())
                        .replyToMessageId(message.getReplyToMessageId())
                        .severTimeStamp(message.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
