package server.cubeTalk.common.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Data
@AllArgsConstructor
public class CommonResponseDto<T>  {
    private int status;
    private String message;
    private T data;

    public static <T> CommonResponseDto<T> success (T data) {
        return new CommonResponseDto<>(HttpStatus.CREATED.value(), "요청처리에 성공했습니다.", data);
    }


}
