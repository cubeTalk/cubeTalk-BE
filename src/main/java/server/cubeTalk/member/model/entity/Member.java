package server.cubeTalk.member.model.entity;


import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import server.cubeTalk.common.entity.BaseTimeStamp;



@Builder
@Document(collection = "member")
public class Member extends BaseTimeStamp {

    @Id
    private String id;
    private String memberId;
    private String nickName;


}
