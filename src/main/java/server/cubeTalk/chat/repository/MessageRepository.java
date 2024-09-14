package server.cubeTalk.chat.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.chat.model.entity.Message;

import java.util.List;
import java.util.Optional;

public interface MessageRepository extends MongoRepository<Message, String> {

    List<Message> findByChannelId(String channelId);

}
