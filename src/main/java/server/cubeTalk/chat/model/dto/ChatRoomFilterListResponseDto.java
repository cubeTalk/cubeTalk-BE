package server.cubeTalk.chat.model.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ChatRoomFilterListResponseDto {
    private String id;
    private String chatMode;
    private String chatStatus;
    private String title;
    private String description;
    private Double chatDuration;
    private String ownerNickName;
    private int maxParticipants;
    private Integer currentParticipantsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

}
