package com.tictactoe.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class StartupVerification {

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
//        log.info("═══════════════════════════════════════════════════════");
//        log.info("🚀 TIC-TAC-TOE MULTIPLAYER SERVER STARTED");
//        log.info("═══════════════════════════════════════════════════════");
//        log.info("📡 WebSocket endpoint: ws://localhost:8081/ws");
//        log.info("🎮 Ready to accept connections!");
//        log.info("═══════════════════════════════════════════════════════");
//
//        // Verify critical beans are loaded
//        log.info("✅ GameController loaded");
//        log.info("✅ GameService loaded");
//        log.info("✅ PlayerService loaded");
//        log.info("✅ WebSocketConfig loaded");
//        log.info("✅ MessageLoggingInterceptor loaded");
//        log.info("✅ WebSocketEventListener loaded");
//        log.info("═══════════════════════════════════════════════════════");
    }
}