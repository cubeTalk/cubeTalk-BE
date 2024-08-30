package server.cubeTalk.member.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import server.cubeTalk.member.model.entity.Member;

import java.util.Optional;

public interface MemberRepository extends MongoRepository<Member,String> {
    Optional<Member> existsByNickName(String nickName);
}
