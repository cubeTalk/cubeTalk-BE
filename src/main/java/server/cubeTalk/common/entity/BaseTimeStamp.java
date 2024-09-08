package server.cubeTalk.common.entity;

import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;


import java.time.LocalDateTime;
import java.time.OffsetDateTime;

@MappedSuperclass
@Getter
public abstract class BaseTimeStamp {

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

}

