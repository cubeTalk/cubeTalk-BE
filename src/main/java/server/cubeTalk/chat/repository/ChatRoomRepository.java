package server.cubeTalk.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.chat.model.entity.ChatRoom;

import java.util.Optional;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
    Optional<ChatRoom> findByChannelId(String channelId);
}
