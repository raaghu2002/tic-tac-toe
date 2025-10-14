package com.tictactoe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class MessageLoggingInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);

        if (accessor.getCommand() != null) {
            StompCommand command = accessor.getCommand();
            String destination = accessor.getDestination();
            String sessionId = accessor.getSessionId();

//            log.info("[MESSAGE] Command: {} | Destination: {} | SessionId: {}",
//                    command, destination, sessionId);

            if (command == StompCommand.SEND) {
//                log.info("[MESSAGE-SEND] Payload: {}", new String((byte[]) message.getPayload()));
            }
        }

        return message;
    }
}