package com.tictactoe.controller;

import com.tictactoe.dto.*;
import com.tictactoe.model.Game;
import com.tictactoe.model.Player;
import com.tictactoe.service.GameService;
import com.tictactoe.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
@Slf4j
public class GameController {

    private final GameService gameService;
    private final PlayerService playerService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/join")
    public void joinGame(JoinGameRequest request, @Header("simpSessionId") String sessionId) {
        if (request == null) {
            return;
        }

        String nickname = request.getNickname();

        if (nickname == null || nickname.trim().isEmpty()) {
            return;
        }

        nickname = nickname.trim();
        log.info("🎮 [JOIN] Player joining: '{}' (sessionId: {})", nickname, sessionId);

        // Register session for disconnect handling
        gameService.registerPlayerSession(nickname, sessionId);

        // Create or get player
        Player player = playerService.createOrGetPlayer(nickname);
        log.info("👤 [JOIN] Player object created/retrieved: {}", player.getNickname());

        // Join matchmaking
        String gameId = gameService.joinMatchmaking(player);
        log.info("🎲 [JOIN] Matchmaking result for {}: gameId={}", nickname, gameId);

        if (gameId != null) {
            // Game found! Notify both players
            Game game = gameService.getGame(gameId);

            log.info("✅ [GAME-CREATED] Game: {} | Player1: {} | Player2: {}",
                    gameId,
                    game.getPlayer1().getNickname(),
                    game.getPlayer2().getNickname());

            // Send matchmaking success to BOTH players
            MatchmakingResponse matchmakingResponse = new MatchmakingResponse();
            matchmakingResponse.setStatus("STARTED");
            matchmakingResponse.setGameId(gameId);
            matchmakingResponse.setMessage("Opponent found! Starting game...");

            String destination1 = "/queue/matchmaking-" + game.getPlayer1().getNickname();
            String destination2 = "/queue/matchmaking-" + game.getPlayer2().getNickname();

            messagingTemplate.convertAndSend(destination1, matchmakingResponse);
            messagingTemplate.convertAndSend(destination2, matchmakingResponse);

            // Small delay to ensure clients subscribe to game topic
            new Thread(() -> {
                try {
                    Thread.sleep(500);

                    GameStateResponse response = buildGameStateResponse(game, "Game started! X goes first.");
                    String gameTopic = "/topic/game/" + gameId;

                    log.info("📤 [GAME-STATE] Sending initial state to: {}", gameTopic);
                    messagingTemplate.convertAndSend(gameTopic, response);
                    log.info("✅ [GAME-STATE] Initial state sent successfully");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("❌ [ERROR] Interrupted while sending initial game state", e);
                }
            }).start();

        } else {
            // Waiting for opponent
            MatchmakingResponse response = new MatchmakingResponse();
            response.setStatus("WAITING");
            response.setMessage("Finding opponent...");

            String destination = "/queue/matchmaking-" + nickname;
            log.info("⏳ [WAITING] Player {} added to queue | Sending to: {}", nickname, destination);

            messagingTemplate.convertAndSend(destination, response);
            log.info("📤 [WAITING] Wait notification sent to {}", nickname);
        }
    }

    @MessageMapping("/cancel")
    public void cancelMatchmaking(CancelMatchmakingRequest request) {
        if (request == null || request.getNickname() == null) {
            return;
        }

        String nickname = request.getNickname().trim();
        log.info("🚫 [CANCEL] Player '{}' canceling matchmaking", nickname);

        gameService.cancelMatchmaking(nickname);

        // Send confirmation
        MatchmakingResponse response = new MatchmakingResponse();
        response.setStatus("CANCELLED");
        response.setMessage("Matchmaking cancelled");

        String destination = "/queue/matchmaking-" + nickname;
        messagingTemplate.convertAndSend(destination, response);
    }

    @MessageMapping("/move")
    public void makeMove(GameMoveRequest request, @Header("simpSessionId") String sessionId) {
        String gameId = request.getGameId();
        String nickname = request.getNickname();
        int row = request.getRow();
        int col = request.getCol();

        log.info("🎯 [MOVE] Player: {} | Position: ({},{}) | Game: {} | Session: {}",
                nickname, row, col, gameId, sessionId);

        boolean success = gameService.makeMove(gameId, nickname, row, col);
        log.info("🎯 [MOVE-RESULT] Success: {} | Player: {}", success, nickname);

        if (success) {
            Game game = gameService.getGame(gameId);

            String message = "Move successful";

            // Check if game is finished
            if (game.getStatus() == Game.GameStatus.FINISHED) {
                String winner = game.getWinner();

                if ("DRAW".equals(winner)) {
                    message = "Game ended in a draw!";
                    playerService.recordDraw(game.getPlayer1().getNickname());
                    playerService.recordDraw(game.getPlayer2().getNickname());
                    log.info("🤝 [GAME-END] Game {} ended in DRAW", gameId);
                } else {
                    Player winnerPlayer = winner.equals("X") ? game.getPlayer1() : game.getPlayer2();
                    Player loserPlayer = winner.equals("X") ? game.getPlayer2() : game.getPlayer1();

                    message = winnerPlayer.getNickname() + " wins!";

                    playerService.recordWin(winnerPlayer.getNickname());
                    playerService.recordLoss(loserPlayer.getNickname());
                    log.info("🏆 [GAME-END] Game {} won by {} ({})", gameId, winnerPlayer.getNickname(), winner);
                }

                // End the game after a delay
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        gameService.endGame(gameId);
                        log.info("🔚 [CLEANUP] Game {} cleaned up", gameId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            GameStateResponse response = buildGameStateResponse(game, message);
            String gameTopic = "/topic/game/" + gameId;

            log.info("📤 [UPDATE] Sending game update to: {} | Turn: {}", gameTopic, game.getCurrentTurn());
            messagingTemplate.convertAndSend(gameTopic, response);
        } else {
            log.warn("❌ [MOVE-FAILED] Invalid move by {} in game {}", nickname, gameId);

            // Send error message back to player
            GameErrorResponse errorResponse = new GameErrorResponse();
            errorResponse.setError("Invalid move");
            errorResponse.setMessage("This move is not allowed");

            String destination = "/queue/error-" + nickname;
            messagingTemplate.convertAndSend(destination, errorResponse);
        }
    }

    @MessageMapping("/heartbeat")
    public void handleHeartbeat(HeartbeatRequest request) {
        if (request != null && request.getNickname() != null) {
            gameService.updatePlayerActivity(request.getNickname());
        }
    }

    @MessageMapping("/forfeit")
    public void forfeitGame(ForfeitGameRequest request) {
        if (request == null || request.getGameId() == null || request.getNickname() == null) {
            return;
        }

        String gameId = request.getGameId();
        String nickname = request.getNickname();

        log.info("🏳️ [FORFEIT] Player '{}' forfeiting game '{}'", nickname, gameId);

        Game game = gameService.getGame(gameId);
        if (game != null && game.getStatus() == Game.GameStatus.IN_PROGRESS) {
            // Determine winner and loser
            String forfeitingPlayerSymbol = game.getPlayerSymbol(nickname);
            String winnerSymbol = forfeitingPlayerSymbol.equals("X") ? "O" : "X";

            Player winner = winnerSymbol.equals("X") ? game.getPlayer1() : game.getPlayer2();
            Player loser = winnerSymbol.equals("X") ? game.getPlayer2() : game.getPlayer1();

            // Update game state
            game.setStatus(Game.GameStatus.FINISHED);
            game.setWinner(winnerSymbol);

            // Record results
            playerService.recordWin(winner.getNickname());
            playerService.recordLoss(loser.getNickname());

            log.info("🏳️ [FORFEIT] Game {} forfeited | Winner: {} | Loser: {}",
                    gameId, winner.getNickname(), loser.getNickname());

            // Send update to both players
            GameStateResponse response = buildGameStateResponse(game,
                    nickname + " forfeited. " + winner.getNickname() + " wins!");
            String gameTopic = "/topic/game/" + gameId;
            messagingTemplate.convertAndSend(gameTopic, response);

            // Clean up game after delay
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    gameService.endGame(gameId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }
    }

    private GameStateResponse buildGameStateResponse(Game game, String message) {
        GameStateResponse response = new GameStateResponse();
        response.setGameId(game.getGameId());
        response.setBoard(game.getBoard());
        response.setCurrentTurn(game.getCurrentTurn());
        response.setStatus(game.getStatus().toString());
        response.setWinner(game.getWinner());
        response.setMessage(message);

        // Get updated player stats
        Player p1 = playerService.getPlayer(game.getPlayer1().getNickname());
        Player p2 = playerService.getPlayer(game.getPlayer2().getNickname());

        if (p1 != null) {
            PlayerInfo p1Info = new PlayerInfo();
            p1Info.setNickname(p1.getNickname());
            p1Info.setSymbol("X");
            p1Info.setWins(p1.getWins());
            p1Info.setLosses(p1.getLosses());
            p1Info.setDraws(p1.getDraws());
            response.setPlayer1(p1Info);
        }

        if (p2 != null) {
            PlayerInfo p2Info = new PlayerInfo();
            p2Info.setNickname(p2.getNickname());
            p2Info.setSymbol("O");
            p2Info.setWins(p2.getWins());
            p2Info.setLosses(p2.getLosses());
            p2Info.setDraws(p2.getDraws());
            response.setPlayer2(p2Info);
        }

        return response;
    }
}