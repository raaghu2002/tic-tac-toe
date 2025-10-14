package com.tictactoe.service;

import com.tictactoe.model.Player;
import com.tictactoe.repository.PlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {

    private final PlayerRepository playerRepository;

    @Transactional
    public Player createOrGetPlayer(String nickname) {
        Optional<Player> existingPlayer = playerRepository.findByNickname(nickname);

        if (existingPlayer.isPresent()) {
//            log.info("Player found: {}", nickname);
            return existingPlayer.get();
        }

        Player newPlayer = new Player();
        newPlayer.setNickname(nickname);
        newPlayer.setWins(0);
        newPlayer.setLosses(0);
        newPlayer.setDraws(0);
        newPlayer.setTotalScore(0);

        Player saved = playerRepository.save(newPlayer);
//        log.info("New player created: {}", nickname);
        return saved;
    }

    @Transactional
    public void recordWin(String nickname) {
        Player player = playerRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Player not found: " + nickname));

        player.addWin();
        playerRepository.save(player);
//        log.info("Win recorded for player: {}", nickname);
    }

    @Transactional
    public void recordLoss(String nickname) {
        Player player = playerRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Player not found: " + nickname));

        player.addLoss();
        playerRepository.save(player);
//        log.info("Loss recorded for player: {}", nickname);
    }

    @Transactional
    public void recordDraw(String nickname) {
        Player player = playerRepository.findByNickname(nickname)
                .orElseThrow(() -> new RuntimeException("Player not found: " + nickname));

        player.addDraw();
        playerRepository.save(player);
//        log.info("Draw recorded for player: {}", nickname);
    }

    public List<Player> getLeaderboard(int limit) {
        List<Player> allPlayers = playerRepository.findTopPlayers();
        return allPlayers.stream()
                .limit(limit)
                .toList();
    }

    public Player getPlayer(String nickname) {
        return playerRepository.findByNickname(nickname).orElse(null);
    }
}