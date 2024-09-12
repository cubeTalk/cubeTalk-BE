package server.cubeTalk.chat.exception;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})  // 클래스에 적용할 수 있도록 설정
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidChatRoomCreateValidator.class)  // 실제 Validator 클래스 지정
public @interface ValidChatRoomCreate {
    String message() default "찬반 토론 모드일 경우, 토론 설정은 필수입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
