package server.cubeTalk.common.config;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;


@Configuration
@EnableScheduling
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${RABBITMQ_HOST}")
    private String rabbitmqHost;

    @Value("${RABBITMQ_PORT}")
    private int rabbitmqPort;

    @Value("${RABBITMQ_USER}")
    private String rabbitmqUser;

    @Value("${RABBITMQ_PASS}")
    private String rabbitmqPass;

    @Value("${RABBITMQ_VHOST}")
    private String rabbitmqVhost;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
            registry.setApplicationDestinationPrefixes("/pub")
                    .enableStompBrokerRelay("/topic")
                    .setRelayHost(rabbitmqHost)
                    .setVirtualHost(rabbitmqVhost)
                    .setRelayPort(rabbitmqPort)
                    .setClientLogin(rabbitmqUser)
                    .setClientPasscode(rabbitmqPass)
                    .setSystemLogin(rabbitmqUser)
                    .setSystemPasscode(rabbitmqPass)
                    .setTaskScheduler(heartBeatScheduler())
                    .setSystemHeartbeatSendInterval(10000)
                    .setSystemHeartbeatReceiveInterval(10000);
    }

    @Bean
    public TaskScheduler heartBeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        return scheduler;
    }


    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry
                .addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS()
                .setDisconnectDelay( 30 * 1000 );
    }

//    @Override
//    public void configureWebSocketTransport(WebSocketTransportRegistration registry) {
//        registry.setSendTimeLimit(20000);  // 메시지를 보내기 위한 시간 제한을 20초로 설정
//        registry.setSendBufferSizeLimit(512 * 1024);  // 512KB 버퍼 크기
//        registry.setTimeToFirstMessage(30000); // 세션 타임아웃
//    }

}
