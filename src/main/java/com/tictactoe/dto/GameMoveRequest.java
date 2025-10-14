package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameMoveRequest {
    private String gameId;
    private String nickname;
    private int row;
    private int col;
}
