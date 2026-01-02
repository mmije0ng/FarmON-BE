package com.backend.farmon.config.chat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

// stomp 연결 config
@RequiredArgsConstructor
@Slf4j
@EnableWebSocketMessageBroker
@Configuration
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final FilterChannelInterceptor filterChannelInterceptor;

    @Value("${spring.rabbitmq.host}")
    private String host;

    @Value("${spring.rabbitmq.username}")
    private String username;

    @Value("${spring.rabbitmq.password}")
    private String password;

    @Value("${spring.rabbitmq.virtual-host}")
    private String virtualHost;

    // sockJS Fallback을 이용해 노출할 endpoint 설정
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry){
        // 웹소켓이 handshake를 하기 위해 연결하는 endpoint
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new StompHandshakeInterceptor()) // StompHandshakeInterceptor 추가
                .withSockJS();

        // SockJS 지원 안 할 시
        registry.addEndpoint("/ws-stomp")
                .setAllowedOriginPatterns("*")
                .addInterceptors(new StompHandshakeInterceptor());
    }

    //메세지 브로커에 관한 설정
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {

        // 메시지 브로커 설정
        registry.setPathMatcher(new AntPathMatcher(".")); // url을 chat/room/3 -> chat.room.3으로 참조하기 위한 설정
        registry.enableStompBrokerRelay("/queue", "/topic", "/exchange", "/amq/queue")
                .setRelayHost(host)
                .setRelayPort(61613)
                .setVirtualHost(virtualHost)
                .setSystemLogin(username)
                .setSystemPasscode(password)
                .setClientLogin(username)
                .setClientPasscode(password);

        // 클라이언트로부터 메시지를 받을 api의 prefix를 설정함
        // publish
        registry.setApplicationDestinationPrefixes("/pub");
    }

//    @Override
//    public void configureMessageBroker(MessageBrokerRegistry registry){
//        registry.enableSimpleBroker("/receive"); // 메시지 수신, 구독 sub
//
//        // 도착 경로에 대한 prefix 설정
//        registry.setApplicationDestinationPrefixes("/send"); // 메시지 송신, pub
//    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(filterChannelInterceptor); // FilterChannelInterceptor 추가
    }
}