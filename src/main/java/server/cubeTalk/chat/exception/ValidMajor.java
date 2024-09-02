package server.cubeTalk.chat.exception;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD) // 해당 애노테이션은 필드에 적용
@Retention(RetentionPolicy.RUNTIME) // 해당 애노테이션은 앱이 실행중에 계속 적용됨
@Constraint(validatedBy = ValidMajorValidator.class) //ValidMajorValidator 클래스 이용하여 검증
public @interface ValidMajor {
    String message() default "올바른 단어가 필요합니다. "; //기본 메세지
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

    String[] word();
}
