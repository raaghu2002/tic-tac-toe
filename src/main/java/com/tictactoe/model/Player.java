package com.tictactoe.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "players")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String nickname;

    @Column(nullable = false)
    private Integer wins = 0;

    @Column(nullable = false)
    private Integer losses = 0;

    @Column(nullable = false)
    private Integer draws = 0;

    @Column(nullable = false)
    private Integer totalScore = 0;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "last_played")
    private LocalDateTime lastPlayed;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastPlayed = LocalDateTime.now();
    }

    public void addWin() {
        this.wins++;
        this.totalScore += 200;
        this.lastPlayed = LocalDateTime.now();
    }

    public void addLoss() {
        this.losses++;
        this.lastPlayed = LocalDateTime.now();
    }

    public void addDraw() {
        this.draws++;
        this.totalScore += 50;
        this.lastPlayed = LocalDateTime.now();
    }

    public String getWinLossDrawRecord() {
        return wins + "/" + losses + "/" + draws;
    }
}