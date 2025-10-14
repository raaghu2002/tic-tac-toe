package com.tictactoe.controller;

import com.tictactoe.dto.*;
import com.tictactoe.model.Game;
import com.tictactoe.model.Player;
import com.tictactoe.service.GameService;
import com.tictactoe.service.PlayerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
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
    public void joinGame(JoinGameRequest request) {
//        log.info("🔵 [RAW-REQUEST] Received join request: {}", request);

        if (request == null) {
//            log.error("❌ [JOIN] Request is NULL!");
            return;
        }

        String nickname = request.getNickname();

        if (nickname == null || nickname.trim().isEmpty()) {
//            log.error("❌ [JOIN] Nickname is NULL or EMPTY! Request: {}", request);
            return;
        }

        nickname = nickname.trim(); // Extra safety
//        log.info("🎮 [JOIN] Player joining: '{}' (length: {})", nickname, nickname.length());

        // Create or get player
        Player player = playerService.createOrGetPlayer(nickname);
//        log.info("👤 [JOIN] Player object created/retrieved: {}", player.getNickname());

        // Join matchmaking
        String gameId = gameService.joinMatchmaking(player);
//        log.info("🎲 [JOIN] Matchmaking result for {}: gameId={}", nickname, gameId);

        if (gameId != null) {
            // Game found! Notify both players with gameId first
            Game game = gameService.getGame(gameId);

//            log.info("✅ [GAME-CREATED] Game: {} | Player1: {} | Player2: {}",
//                    gameId,
//                    game.getPlayer1().getNickname(),
//                    game.getPlayer2().getNickname());

            // Send matchmaking success to BOTH players with gameId
            MatchmakingResponse matchmakingResponse = new MatchmakingResponse();
            matchmakingResponse.setStatus("STARTED");
            matchmakingResponse.setGameId(gameId);
            matchmakingResponse.setMessage("Opponent found! Starting game...");

            String destination1 = "/queue/matchmaking-" + game.getPlayer1().getNickname();
            String destination2 = "/queue/matchmaking-" + game.getPlayer2().getNickname();

//            log.info("📤 [NOTIFY-P1] Sending to: {} | Player: {} | GameId: {}",
//                    destination1, game.getPlayer1().getNickname(), gameId);
//            messagingTemplate.convertAndSend(destination1, matchmakingResponse);

//            log.info("📤 [NOTIFY-P2] Sending to: {} | Player: {} | GameId: {}",
//                    destination2, game.getPlayer2().getNickname(), gameId);
//            messagingTemplate.convertAndSend(destination2, matchmakingResponse);

            // Small delay to ensure clients subscribe to game topic
            new Thread(() -> {
                try {
                    Thread.sleep(500);

                    GameStateResponse response = buildGameStateResponse(game, "Game started!");
                    String gameTopic = "/topic/game/" + gameId;

//                    log.info("📤 [GAME-STATE] Sending initial state to: {}", gameTopic);
//                    log.info("📊 [GAME-STATE] Board: {} | Turn: {} | Status: {}",
//                            java.util.Arrays.deepToString(game.getBoard()),
//                            game.getCurrentTurn(),
//                            game.getStatus());

                    messagingTemplate.convertAndSend(gameTopic, response);
//                    log.info("✅ [GAME-STATE] Initial state sent successfully");

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
//                    log.error("❌ [ERROR] Interrupted while sending initial game state", e);
                }
            }).start();

        } else {
            // Waiting for opponent
            MatchmakingResponse response = new MatchmakingResponse();
            response.setStatus("WAITING");
            response.setMessage("Finding opponent...");

            String destination = "/queue/matchmaking-" + nickname;
//            log.info("⏳ [WAITING] Player {} added to queue | Sending to: {}", nickname, destination);

            messagingTemplate.convertAndSend(destination, response);
//            log.info("📤 [WAITING] Wait notification sent to {}", nickname);
        }
    }

    @MessageMapping("/move")
    public void makeMove(GameMoveRequest request) {
        String gameId = request.getGameId();
        String nickname = request.getNickname();
        int row = request.getRow();
        int col = request.getCol();

//        log.info("🎯 [MOVE] Player: {} | Position: ({},{}) | Game: {}", nickname, row, col, gameId);

        boolean success = gameService.makeMove(gameId, nickname, row, col);
//        log.info("🎯 [MOVE-RESULT] Success: {} | Player: {}", success, nickname);

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
//                    log.info("🤝 [GAME-END] Game {} ended in DRAW", gameId);
                } else {
                    Player winnerPlayer = winner.equals("X") ? game.getPlayer1() : game.getPlayer2();
                    Player loserPlayer = winner.equals("X") ? game.getPlayer2() : game.getPlayer1();

                    message = winnerPlayer.getNickname() + " wins!";

                    playerService.recordWin(winnerPlayer.getNickname());
                    playerService.recordLoss(loserPlayer.getNickname());
//                    log.info("🏆 [GAME-END] Game {} won by {} ({})", gameId, winnerPlayer.getNickname(), winner);
                }

                // End the game after a delay
                new Thread(() -> {
                    try {
                        Thread.sleep(5000);
                        gameService.endGame(gameId);
//                        log.info("🔚 [CLEANUP] Game {} cleaned up", gameId);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }).start();
            }

            GameStateResponse response = buildGameStateResponse(game, message);
            String gameTopic = "/topic/game/" + gameId;

//            log.info("📤 [UPDATE] Sending game update to: {} | Turn: {}", gameTopic, game.getCurrentTurn());
            messagingTemplate.convertAndSend(gameTopic, response);
        } else {
//            log.warn("❌ [MOVE-FAILED] Invalid move by {} in game {}", nickname, gameId);
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
//            log.debug("📊 [STATS] Player1: {} | W/L/D: {}/{}/{}", p1.getNickname(), p1.getWins(), p1.getLosses(), p1.getDraws());
        }

        if (p2 != null) {
            PlayerInfo p2Info = new PlayerInfo();
            p2Info.setNickname(p2.getNickname());
            p2Info.setSymbol("O");
            p2Info.setWins(p2.getWins());
            p2Info.setLosses(p2.getLosses());
            p2Info.setDraws(p2.getDraws());
            response.setPlayer2(p2Info);
//            log.debug("📊 [STATS] Player2: {} | W/L/D: {}/{}/{}", p2.getNickname(), p2.getWins(), p2.getLosses(), p2.getDraws());
        }

        return response;
    }
}