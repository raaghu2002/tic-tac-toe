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
    private final Map<String, LocalDateTime> playerLastActivity = new ConcurrentHashMap<>();

    // Track player sessions
    private final Map<String, String> playerSessions = new ConcurrentHashMap<>();

    // Constants
    private static final int STALE_PLAYER_TIMEOUT_SECONDS = 60;
    private static final int INACTIVE_PLAYER_TIMEOUT_SECONDS = 180; // 3 minutes
    private static final int GAME_TIMEOUT_MINUTES = 10;

    public synchronized String joinMatchmaking(Player player) {
        String nickname = player.getNickname();

        log.info("🔍 [MATCHMAKING] Player '{}' requesting to join", nickname);
        log.info("📊 [MATCHMAKING] Current state - Waiting: {}, Active Games: {}",
                waitingPlayers.size(), activeGames.size());

        // Update player activity
        updatePlayerActivity(nickname);

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
                removePlayerFromGame(nickname);
            }
        }

        // Check if player is already in waiting queue
        if (waitingPlayers.contains(nickname)) {
            log.warn("⚠️ [MATCHMAKING] Player '{}' already in waiting queue, skipping", nickname);
            return null;
        }

        // Clean up stale and inactive players
        cleanupStalePlayers();
        cleanupInactivePlayers();

        // Check if there's a waiting player (but not the same player!)
        if (!waitingPlayers.isEmpty()) {
            String waitingPlayerNickname = waitingPlayers.poll();

            // CRITICAL FIX: Prevent self-matching
            if (waitingPlayerNickname.equals(nickname)) {
                log.error("❌ [MATCHMAKING] Prevented self-matching for player '{}'", nickname);
                waitingPlayers.offer(nickname);
                playerJoinTime.put(nickname, LocalDateTime.now());
                return null;
            }

            log.info("🤝 [MATCHMAKING] Found waiting player: '{}'", waitingPlayerNickname);
            log.info("🤝 [MATCHMAKING] Pairing '{}' with '{}'", waitingPlayerNickname, nickname);

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

    /**
     * Remove stale players from waiting queue (over 60 seconds old)
     */
    private void cleanupStalePlayers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(STALE_PLAYER_TIMEOUT_SECONDS);
        List<String> toRemove = new ArrayList<>();

        for (String nickname : waitingPlayers) {
            LocalDateTime joinTime = playerJoinTime.get(nickname);
            if (joinTime != null && joinTime.isBefore(cutoffTime)) {
                toRemove.add(nickname);
                log.info("🧹 [CLEANUP] Removing stale player from queue: '{}'", nickname);
            }
        }

        for (String nickname : toRemove) {
            removePlayerFromQueue(nickname);
        }
    }

    /**
     * Remove inactive players from games (no activity for 3 minutes)
     */
    private void cleanupInactivePlayers() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusSeconds(INACTIVE_PLAYER_TIMEOUT_SECONDS);
        List<String> gamesToEnd = new ArrayList<>();

        for (Map.Entry<String, Game> entry : activeGames.entrySet()) {
            Game game = entry.getValue();
            String gameId = entry.getKey();

            if (game.getStatus() != Game.GameStatus.IN_PROGRESS) {
                continue;
            }

            // Check if game has timed out (no moves for too long)
            if (game.getLastMoveAt() != null &&
                    game.getLastMoveAt().isBefore(LocalDateTime.now().minusMinutes(GAME_TIMEOUT_MINUTES))) {
                log.info("⏰ [CLEANUP] Game '{}' timed out", gameId);
                gamesToEnd.add(gameId);
                continue;
            }

            // Check player inactivity
            boolean player1Inactive = isPlayerInactive(game.getPlayer1().getNickname(), cutoffTime);
            boolean player2Inactive = isPlayerInactive(game.getPlayer2().getNickname(), cutoffTime);

            if (player1Inactive || player2Inactive) {
                log.info("⏰ [CLEANUP] Game '{}' has inactive player(s)", gameId);
                game.setStatus(Game.GameStatus.ABANDONED);
                gamesToEnd.add(gameId);
            }
        }

        for (String gameId : gamesToEnd) {
            endGame(gameId);
        }
    }

    private boolean isPlayerInactive(String nickname, LocalDateTime cutoffTime) {
        LocalDateTime lastActivity = playerLastActivity.get(nickname);
        return lastActivity == null || lastActivity.isBefore(cutoffTime);
    }

    public void updatePlayerActivity(String nickname) {
        playerLastActivity.put(nickname, LocalDateTime.now());
    }

    public Game getGame(String gameId) {
        Game game = activeGames.get(gameId);
        log.debug("🔍 [GET-GAME] Requested gameId: {} | Found: {}", gameId, (game != null));
        return game;
    }

    public boolean makeMove(String gameId, String nickname, int row, int col) {
        log.info("🎯 [MAKE-MOVE] GameId: {} | Player: {} | Position: ({},{})", gameId, nickname, row, col);

        // Update player activity
        updatePlayerActivity(nickname);

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
                removePlayerFromGame(p1Nick);
                log.info("🔚 [END-GAME] Removed player1 mapping: {}", p1Nick);
            }
            if (game.getPlayer2() != null) {
                String p2Nick = game.getPlayer2().getNickname();
                removePlayerFromGame(p2Nick);
                log.info("🔚 [END-GAME] Removed player2 mapping: {}", p2Nick);
            }

            // Remove game
            activeGames.remove(gameId);
            log.info("🔚 [END-GAME] Game removed: {} | Active games remaining: {}", gameId, activeGames.size());
        } else {
            log.warn("⚠️ [END-GAME] Game not found: {}", gameId);
        }
    }

    /**
     * Cancel matchmaking - removes player from queue
     */
    public void cancelMatchmaking(String nickname) {
        log.info("🚫 [CANCEL] Player '{}' canceling matchmaking", nickname);
        removePlayerFromQueue(nickname);
    }

    /**
     * Remove player from waiting queue
     */
    public void removePlayerFromQueue(String nickname) {
        boolean removed = waitingPlayers.remove(nickname);
        playerJoinTime.remove(nickname);
        playerLastActivity.remove(nickname);

        log.info("🚫 [REMOVE-QUEUE] Player '{}' removed from queue: {} | Waiting players: {}",
                nickname, removed, waitingPlayers.size());
    }

    /**
     * Remove player from game mappings
     */
    private void removePlayerFromGame(String nickname) {
        playerToGameMap.remove(nickname);
        playerJoinTime.remove(nickname);
        playerSessions.remove(nickname);
    }

    /**
     * Handle player disconnect - clean up all references
     */
    public void handlePlayerDisconnect(String nickname) {
        log.info("🔌 [DISCONNECT] Player '{}' disconnected", nickname);

        // Remove from waiting queue
        removePlayerFromQueue(nickname);

        // Check if player is in an active game
        String gameId = playerToGameMap.get(nickname);
        if (gameId != null) {
            Game game = activeGames.get(gameId);
            if (game != null && game.getStatus() == Game.GameStatus.IN_PROGRESS) {
                log.info("🔌 [DISCONNECT] Marking game '{}' as abandoned", gameId);
                game.setStatus(Game.GameStatus.ABANDONED);

                // Notify the other player that opponent disconnected
                String opponentNickname = getOpponentNickname(game, nickname);
                if (opponentNickname != null) {
                    log.info("🔌 [DISCONNECT] Opponent in game: '{}'", opponentNickname);
                }
            }
        }

        // Clean up all player data
        removePlayerFromGame(nickname);
    }

    private String getOpponentNickname(Game game, String playerNickname) {
        if (game.getPlayer1() != null && game.getPlayer1().getNickname().equals(playerNickname)) {
            return game.getPlayer2() != null ? game.getPlayer2().getNickname() : null;
        } else if (game.getPlayer2() != null && game.getPlayer2().getNickname().equals(playerNickname)) {
            return game.getPlayer1() != null ? game.getPlayer1().getNickname() : null;
        }
        return null;
    }

    /**
     * Register player session (called on WebSocket connect)
     */
    public void registerPlayerSession(String nickname, String sessionId) {
        playerSessions.put(nickname, sessionId);
        updatePlayerActivity(nickname);
        log.info("📝 [SESSION] Registered session for player '{}': {}", nickname, sessionId);
    }

    /**
     * Unregister player session (called on WebSocket disconnect)
     */
    public void unregisterPlayerSession(String sessionId) {
        String nickname = null;
        for (Map.Entry<String, String> entry : playerSessions.entrySet()) {
            if (entry.getValue().equals(sessionId)) {
                nickname = entry.getKey();
                break;
            }
        }

        if (nickname != null) {
            log.info("📝 [SESSION] Unregistering session for player '{}': {}", nickname, sessionId);
            handlePlayerDisconnect(nickname);
        }
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

    /**
     * Get statistics for monitoring
     */
    public Map<String, Object> getDetailedStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeGames", activeGames.size());
        stats.put("waitingPlayers", waitingPlayers.size());
        stats.put("activeSessions", playerSessions.size());
        stats.put("playersInGames", playerToGameMap.size());
        return stats;
    }

    /**
     * Check if a player is currently in a game
     */
    public boolean isPlayerInGame(String nickname) {
        return playerToGameMap.containsKey(nickname);
    }

    /**
     * Check if a player is in waiting queue
     */
    public boolean isPlayerWaiting(String nickname) {
        return waitingPlayers.contains(nickname);
    }
}