package server.cubeTalk.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.chat.model.entity.ChatRoom;

public interface ChatRoomRepository extends MongoRepository<ChatRoom, String> {
}
