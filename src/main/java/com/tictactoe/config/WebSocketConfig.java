package com.tictactoe.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final MessageLoggingInterceptor messageLoggingInterceptor;

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        log.info("🔧 [CONFIG] Configuring message broker");

        // Enable simple broker for /topic and /queue
        config.enableSimpleBroker("/topic", "/queue");

        // Set application destination prefix
        config.setApplicationDestinationPrefixes("/app");

        // IMPORTANT: Set user destination prefix (default is "/user")
        config.setUserDestinationPrefix("/user");

        log.info("✅ [CONFIG] Message broker configured:");
        log.info("   - Simple broker: /topic, /queue");
        log.info("   - App prefix: /app");
        log.info("   - User prefix: /user");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        log.info("🔧 [CONFIG] Registering STOMP endpoints");

        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();

        log.info("✅ [CONFIG] STOMP endpoint registered: /ws with SockJS");
    }
}