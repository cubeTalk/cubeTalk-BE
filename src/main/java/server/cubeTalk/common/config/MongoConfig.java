package server.cubeTalk.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.Optional;

@Configuration
@EnableMongoAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class MongoConfig {

    // 커스텀 DateTimeProvider를 사용해 Auditing 시 서울 시간대 적용
    @Bean(name = "auditingDateTimeProvider")
    public DateTimeProvider dateTimeProvider() {
        return () -> Optional.of(LocalDateTime.now(ZoneId.of("Asia/Seoul")));
    }
}
