package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardEntry {
    private String nickname;
    private Integer wins;
    private Integer losses;
    private Integer draws;
    private String record;
    private Integer totalScore;
}
