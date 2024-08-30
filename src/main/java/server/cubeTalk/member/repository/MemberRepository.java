package server.cubeTalk.member.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.member.model.entity.Member;

public interface MemberRepository extends MongoRepository<Member,String> {
}
