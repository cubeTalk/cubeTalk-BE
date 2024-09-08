package server.cubeTalk.common.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;

@Data
@AllArgsConstructor
public class CommonResponseDto<T>  {
    private int status;
    private String message;
    private T data;

    public static <T> CommonResponseDto<T> success (T data) {
        return new CommonResponseDto<>(HttpStatus.OK.value(), "요청처리에 성공했습니다.", data);
    }

    public static <T> CommonResponseDto<T> fail(T data) {
        return new CommonResponseDto<>(400, "유효성 검사에 실패했습니다.", data);
    }

    public record CommonResponseErrorDto(int status, String message) {
        public static CommonResponseErrorDto error(int status, String message) {
            return new CommonResponseErrorDto(status, message);
        }
    }

    public record CommonResponseSuccessDto(int status, String message) {
        public static CommonResponseSuccessDto success(int status, String message) {
            return new CommonResponseSuccessDto(status,message);
        }
    }





}
