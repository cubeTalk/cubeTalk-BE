package server.cubeTalk.common.exception;


import lombok.RequiredArgsConstructor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import server.cubeTalk.common.dto.CommonResponseDto;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final SimpMessagingTemplate messagingTemplate;

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<CommonResponseDto.CommonResponseErrorDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        CommonResponseDto.CommonResponseErrorDto response = CommonResponseDto.CommonResponseErrorDto.error(400, ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<CommonResponseDto.CommonResponseErrorDto> handleRuntimeException(RuntimeException ex) {
        CommonResponseDto.CommonResponseErrorDto response = CommonResponseDto.CommonResponseErrorDto.error(HttpStatus.INTERNAL_SERVER_ERROR.value(),ex.getMessage());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /* 유효성 검증 실패에 대한 에러 커스텀 처리 반환 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponseDto<Map<String, String>>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        return new ResponseEntity<>(new CommonResponseDto<>(400, "유효성 검사 실패", errors), HttpStatus.BAD_REQUEST);
    }


    @MessageExceptionHandler(MethodArgumentNotValidException.class)
    public void handleValidationException(MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.put(error.getField(), error.getDefaultMessage());
        }

        // CommonResponseSocketValidDto를 사용하여 응답 메시지 생성
        CommonResponseDto.CommonResponseSocketValidDto<Map<String, String>> response =
                CommonResponseDto.CommonResponseSocketValidDto.socketValid("유효성 검사 실패", errors);

        messagingTemplate.convertAndSend("/topic/error", response);
    }
}
