package com.tictactoe.controller;

import com.tictactoe.dto.LeaderboardEntry;
import com.tictactoe.model.Player;
import com.tictactoe.service.GameService;
import com.tictactoe.service.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class RestApiController {

    private final PlayerService playerService;
    private final GameService gameService;

    @GetMapping("/leaderboard")
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard(
            @RequestParam(defaultValue = "10") int limit) {

        List<Player> topPlayers = playerService.getLeaderboard(limit);

        List<LeaderboardEntry> leaderboard = topPlayers.stream()
                .map(player -> new LeaderboardEntry(
                        player.getNickname(),
                        player.getWins(),
                        player.getLosses(),
                        player.getDraws(),
                        player.getWinLossDrawRecord(),
                        player.getTotalScore()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(leaderboard);
    }

    @GetMapping("/player/{nickname}")
    public ResponseEntity<Player> getPlayer(@PathVariable String nickname) {
        Player player = playerService.getPlayer(nickname);

        if (player != null) {
            return ResponseEntity.ok(player);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getGameStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeGames", gameService.getActiveGamesCount());
        stats.put("waitingPlayers", gameService.getWaitingPlayersCount());

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "TicTacToe Multiplayer");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/test")
    public ResponseEntity<Map<String, String>> test() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "OK");
        response.put("message", "Server is running!");
        response.put("timestamp", java.time.LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to clear matchmaking queue
     * USE WITH CAUTION: This will remove all waiting players
     */
    @DeleteMapping("/admin/queue/clear")
    public ResponseEntity<Map<String, Object>> clearQueue() {
        int removedCount = gameService.clearWaitingQueue();

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Queue cleared successfully");
        response.put("removedPlayers", removedCount);
        response.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(response);
    }

    /**
     * Admin endpoint to get detailed queue information
     */
    @GetMapping("/admin/queue/details")
    public ResponseEntity<Map<String, Object>> getQueueDetails() {
        Map<String, Object> details = gameService.getDetailedStats();
        List<String> waitingPlayers = gameService.getWaitingPlayersList();

        details.put("waitingPlayersList", waitingPlayers);
        details.put("timestamp", LocalDateTime.now());

        return ResponseEntity.ok(details);
    }

    /**
     * Admin endpoint to remove specific player from queue
     */
    @DeleteMapping("/admin/queue/remove/{nickname}")
    public ResponseEntity<Map<String, String>> removePlayerFromQueue(@PathVariable String nickname) {
        gameService.removePlayerFromQueue(nickname);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", "Player removed from queue");
        response.put("nickname", nickname);

        return ResponseEntity.ok(response);
    }
}