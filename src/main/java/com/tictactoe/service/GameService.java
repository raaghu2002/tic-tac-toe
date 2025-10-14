package com.tictactoe.service;

import com.tictactoe.model.Game;
import com.tictactoe.model.Player;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class GameService {

    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    private final Queue<String> waitingPlayers = new LinkedList<>();
    private final Map<String, String> playerToGameMap = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> playerJoinTime = new ConcurrentHashMap<>();

    public synchronized String joinMatchmaking(Player player) {
        String nickname = player.getNickname();

        log.info("🔍 [MATCHMAKING] Player '{}' requesting to join", nickname);
        log.info("📊 [MATCHMAKING] Current state - Waiting: {}, Active Games: {}",
                waitingPlayers.size(), activeGames.size());

        // Check if player is already in a game
        if (playerToGameMap.containsKey(nickname)) {
            String existingGameId = playerToGameMap.get(nickname);
            Game existingGame = activeGames.get(existingGameId);

            log.info("🔄 [MATCHMAKING] Player '{}' already mapped to game '{}'", nickname, existingGameId);

            if (existingGame != null && existingGame.getStatus() == Game.GameStatus.IN_PROGRESS) {
                log.info("✅ [MATCHMAKING] Returning existing game '{}' for player '{}'", existingGameId, nickname);
                return existingGameId;
            } else {
                log.warn("⚠️ [MATCHMAKING] Existing game '{}' is null or finished, removing mapping", existingGameId);
                playerToGameMap.remove(nickname);
                playerJoinTime.remove(nickname);
            }
        }

        // Check if player is already in waiting queue
        if (waitingPlayers.contains(nickname)) {
            log.warn("⚠️ [MATCHMAKING] Player '{}' already in waiting queue, skipping", nickname);
            return null;
        }

        // Clean up stale waiting players (over 60 seconds old)
        cleanupStaleWaitingPlayers();

        // Check if there's a waiting player (but not the same player!)
        if (!waitingPlayers.isEmpty()) {
            String waitingPlayerNickname = waitingPlayers.poll();

            // CRITICAL FIX: Prevent self-matching
            if (waitingPlayerNickname.equals(nickname)) {
                log.error("❌ [MATCHMAKING] Prevented self-matching for player '{}'", nickname);
                // Put player back in queue
                waitingPlayers.offer(nickname);
                playerJoinTime.put(nickname, LocalDateTime.now());
                return null;
            }

            log.info("🤝 [MATCHMAKING] Found waiting player: '{}'", waitingPlayerNickname);
            log.info("🤝 [MATCHMAKING] Pairing '{}' with '{}'", waitingPlayerNickname, nickname);

            // Verify both players are different
            if (waitingPlayerNickname.equals(nickname)) {
                log.error("❌ [CRITICAL] Self-matching detected and prevented!");
                waitingPlayers.offer(nickname);
                playerJoinTime.put(nickname, LocalDateTime.now());
                return null;
            }

            // Remove from join time tracking
            playerJoinTime.remove(waitingPlayerNickname);
            playerJoinTime.remove(nickname);

            // Create new game
            String gameId = UUID.randomUUID().toString();
            Game game = new Game(gameId);

            // Find waiting player
            Player waitingPlayer = new Player();
            waitingPlayer.setNickname(waitingPlayerNickname);

            game.setPlayer1(waitingPlayer);
            game.setPlayer2(player);
            game.setStatus(Game.GameStatus.IN_PROGRESS);

            activeGames.put(gameId, game);
            playerToGameMap.put(waitingPlayerNickname, gameId);
            playerToGameMap.put(nickname, gameId);

            log.info("🎮 [GAME-CREATED] GameId: {}", gameId);
            log.info("🎮 [GAME-CREATED] Player1 (X): '{}'", waitingPlayerNickname);
            log.info("🎮 [GAME-CREATED] Player2 (O): '{}'", nickname);
            log.info("📊 [GAME-CREATED] Active games now: {}", activeGames.size());

            return gameId;
        } else {
            // Add to waiting queue
            waitingPlayers.offer(nickname);
            playerJoinTime.put(nickname, LocalDateTime.now());
            log.info("⏳ [MATCHMAKING] No opponent found, adding '{}' to waiting queue", nickname);
            log.info("📊 [MATCHMAKING] Waiting queue size: {}", waitingPlayers.size());
            return null;
        }
    }

    private void cleanupStaleWaitingPlayers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(60);
        List<String> toRemove = new ArrayList<>();

        for (String nickname : waitingPlayers) {
            LocalDateTime joinTime = playerJoinTime.get(nickname);
            if (joinTime != null && joinTime.isBefore(cutoffTime)) {
                toRemove.add(nickname);
                log.info("🧹 [CLEANUP] Removing stale player from queue: '{}'", nickname);
            }
        }

        for (String nickname : toRemove) {
            waitingPlayers.remove(nickname);
            playerJoinTime.remove(nickname);
        }
    }

    public Game getGame(String gameId) {
        Game game = activeGames.get(gameId);
        log.debug("🔍 [GET-GAME] Requested gameId: {} | Found: {}", gameId, (game != null));
        return game;
    }

    public boolean makeMove(String gameId, String nickname, int row, int col) {
        log.info("🎯 [MAKE-MOVE] GameId: {} | Player: {} | Position: ({},{})", gameId, nickname, row, col);

        Game game = activeGames.get(gameId);
        if (game == null) {
            log.error("❌ [MAKE-MOVE] Game not found: {}", gameId);
            log.error("❌ [MAKE-MOVE] Active games: {}", activeGames.keySet());
            return false;
        }

        String symbol = game.getPlayerSymbol(nickname);
        if (symbol == null) {
            log.error("❌ [MAKE-MOVE] Player '{}' not in game '{}'", nickname, gameId);
            log.error("❌ [MAKE-MOVE] Game has players: {} and {}",
                    game.getPlayer1() != null ? game.getPlayer1().getNickname() : "null",
                    game.getPlayer2() != null ? game.getPlayer2().getNickname() : "null");
            return false;
        }

        log.info("✅ [MAKE-MOVE] Player '{}' has symbol '{}'", nickname, symbol);

        boolean moveSuccess = game.makeMove(row, col, symbol);

        if (moveSuccess) {
            log.info("✅ [MAKE-MOVE] Move successful | Current turn: {} | Status: {}",
                    game.getCurrentTurn(), game.getStatus());
        } else {
            log.warn("❌ [MAKE-MOVE] Move failed | Current turn: {} | Cell occupied: {}",
                    game.getCurrentTurn(), game.getBoard()[row][col]);
        }

        return moveSuccess;
    }

    public void endGame(String gameId) {
        log.info("🔚 [END-GAME] Ending game: {}", gameId);

        Game game = activeGames.get(gameId);
        if (game != null) {
            // Remove player mappings
            if (game.getPlayer1() != null) {
                String p1Nick = game.getPlayer1().getNickname();
                playerToGameMap.remove(p1Nick);
                playerJoinTime.remove(p1Nick);
                log.info("🔚 [END-GAME] Removed player1 mapping: {}", p1Nick);
            }
            if (game.getPlayer2() != null) {
                String p2Nick = game.getPlayer2().getNickname();
                playerToGameMap.remove(p2Nick);
                playerJoinTime.remove(p2Nick);
                log.info("🔚 [END-GAME] Removed player2 mapping: {}", p2Nick);
            }

            // Remove game
            activeGames.remove(gameId);
            log.info("🔚 [END-GAME] Game removed: {} | Active games remaining: {}", gameId, activeGames.size());
        } else {
            log.warn("⚠️ [END-GAME] Game not found: {}", gameId);
        }
    }

    public void cancelMatchmaking(String nickname) {
        log.info("🚫 [CANCEL] Player '{}' canceling matchmaking", nickname);

        boolean removed = waitingPlayers.remove(nickname);
        playerToGameMap.remove(nickname);
        playerJoinTime.remove(nickname);

        log.info("🚫 [CANCEL] Removed from queue: {} | Waiting players: {}", removed, waitingPlayers.size());
    }

    public int getActiveGamesCount() {
        int count = activeGames.size();
        log.debug("📊 [STATS] Active games count: {}", count);
        return count;
    }

    public int getWaitingPlayersCount() {
        int count = waitingPlayers.size();
        log.debug("📊 [STATS] Waiting players count: {}", count);
        return count;
    }

    public List<String> getWaitingPlayersList() {
        return new ArrayList<>(waitingPlayers);
    }
}