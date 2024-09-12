package server.cubeTalk.chat.exception;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import server.cubeTalk.chat.model.dto.ChatRoomCreateRequestDto;

import java.util.Arrays;

public class ValidChatRoomCreateValidator implements ConstraintValidator<ValidChatRoomCreate, ChatRoomCreateRequestDto> {

    @Override
    public boolean isValid(ChatRoomCreateRequestDto dto, ConstraintValidatorContext context) {
        boolean isValid = true;

        // 채팅방 모드가 "찬반"일 때 debateSettings가 존재하는지 확인
        if ("찬반".equals(dto.getChatMode()) && (!dto.getDebateSettings().isPresent())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("찬반 토론일 경우, 토론 설정은 필수입니다.")
                    .addPropertyNode("debateSettings").addConstraintViolation();
            isValid = false;
        }

        // 채팅방 모드가 "자유"일 때 chatDuration이 반드시 존재해야 함
        if ("자유".equals(dto.getChatMode()) && (!dto.getChatDuration().isPresent())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("자유 모드일 경우, chatDuration은 필수입니다.")
                    .addPropertyNode("chatDuration").addConstraintViolation();
            isValid = false;
        }

        if ((dto.getMaxParticipants() % 2 != 0 || dto.getMaxParticipants() < 6) && "찬반".equals(dto.getChatMode())) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("찬반 토론일 경우, 사용자 수(수용인원)은 짝수면서 최소 6명이상이어야합니다.")
                    .addPropertyNode("maxParticipants").addConstraintViolation();
            isValid = false;
        }

        return isValid;
    }


}
