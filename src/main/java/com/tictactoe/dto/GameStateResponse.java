package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GameStateResponse {
    private String gameId;
    private char[][] board;
    private String currentTurn;
    private String status;
    private String winner;
    private PlayerInfo player1;
    private PlayerInfo player2;
    private String message;
}
