package server.cubeTalk.chat.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.chat.model.entity.ChatRoom;

import java.util.List;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {

    ChatRoom findByChannelId(String channelId);

    List<ChatRoom> findByChatStatus(String status);

    // 채팅방의 모드와 상태에 따라 채팅방 목록을 페이지네이션하여 반환

    Page<ChatRoom> findByChatModeAndChatStatus(String chatMode, String status, Pageable pageable);

    // 채팅방의 모드에 따른 전체 목록을 페이지네이션하여 반환
    Page<ChatRoom> findByChatMode(String chatMode, Pageable pageable);

    Page<ChatRoom> findByChatStatus(String status, Pageable pageable);
}

