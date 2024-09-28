package server.cubeTalk.chat.model.dto;


import com.fasterxml.jackson.annotation.JsonInclude;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import lombok.NoArgsConstructor;
import server.cubeTalk.chat.exception.ValidMajor;

import java.util.Optional;

@Data
@NoArgsConstructor
public class ChatRoomJoinRequestDto {
    @Pattern(regexp = "^\\S.*$", message = "닉네임은 공백만 입력할 수 없습니다.")
    private String nickName;
    @NotEmpty(message = "역할은 비워둘 수 없습니다.")
    @ValidMajor(word = {"찬성", "반대", "관전", "자유"}, message = "내용에 '찬성 or 반대 or 관전 or 자유'이 포함되어야 합니다.")
    private String role;
    private Optional<String> ownerId = Optional.empty();
}
