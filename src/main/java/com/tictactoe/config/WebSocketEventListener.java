package com.tictactoe.config;

import com.tictactoe.service.GameService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

/**
 * WebSocket Event Listener to handle player connections and disconnections
 * This fixes the bug where players remain in queue after leaving the site
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final GameService gameService;

    @EventListener
    public void handleWebSocketConnectListener(SessionConnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("🔌 [WEBSOCKET] New WebSocket connection: sessionId={}", sessionId);
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();

        log.info("🔌 [WEBSOCKET] WebSocket disconnection: sessionId={}", sessionId);

        // Clean up player from queues and games
        gameService.unregisterPlayerSession(sessionId);
    }

    @EventListener
    public void handleSessionSubscribeEvent(SessionSubscribeEvent event) {
        StompHeaderAccessor headerAccessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = headerAccessor.getSessionId();
        String destination = headerAccessor.getDestination();

        log.debug("📝 [WEBSOCKET] Session subscribed: sessionId={}, destination={}", sessionId, destination);
    }
}