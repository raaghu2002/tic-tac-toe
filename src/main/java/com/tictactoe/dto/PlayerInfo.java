package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PlayerInfo {
    private String nickname;
    private String symbol;
    private Integer wins;
    private Integer losses;
    private Integer draws;
}
