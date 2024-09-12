package server.cubeTalk.chat.exception;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;

import java.util.Arrays;

public class ValidChatRoomCreateValidator implements ConstraintValidator<ValidChatRoomCreate, ChatRoomCreateRequestDto> {

    @Override
    public boolean isValid(ChatRoomCreateRequestDto dto, ConstraintValidatorContext context) {
        // 채팅방 모드가 "찬반"일 때 debateSettings dto가 존재하는지 확인
        if ("찬반".equals(dto.getChatMode()) && (!dto.getDebateSettings().isPresent())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("찬반 토론일 경우, 토론 설정은 필수입니다.")
                    .addPropertyNode("debateSettings").addConstraintViolation();
            return false;
        }
        return true;
    }


}
