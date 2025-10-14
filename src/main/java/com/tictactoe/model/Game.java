package com.tictactoe.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Arrays;

@Data
public class Game {

    private String gameId;
    private Player player1;
    private Player player2;
    private String currentTurn; // "X" or "O"
    private char[][] board;
    private GameStatus status;
    private String winner; // null, "X", "O", or "DRAW"
    private LocalDateTime createdAt;
    private LocalDateTime lastMoveAt;

    public enum GameStatus {
        WAITING, IN_PROGRESS, FINISHED, ABANDONED
    }

    public Game(String gameId) {
        this.gameId = gameId;
        this.board = new char[3][3];
        for (int i = 0; i < 3; i++) {
            Arrays.fill(board[i], ' ');
        }
        this.status = GameStatus.WAITING;
        this.currentTurn = "X";
        this.createdAt = LocalDateTime.now();
        this.lastMoveAt = LocalDateTime.now();
    }

    public boolean makeMove(int row, int col, String symbol) {
        if (row < 0 || row > 2 || col < 0 || col > 2) {
            return false;
        }

        if (board[row][col] != ' ') {
            return false;
        }

        if (!currentTurn.equals(symbol)) {
            return false;
        }

        board[row][col] = symbol.charAt(0);
        lastMoveAt = LocalDateTime.now();

        // Switch turn
        currentTurn = currentTurn.equals("X") ? "O" : "X";

        // Check for winner or draw
        checkGameStatus();

        return true;
    }

    private void checkGameStatus() {
        // Check rows
        for (int i = 0; i < 3; i++) {
            if (board[i][0] != ' ' && board[i][0] == board[i][1] && board[i][1] == board[i][2]) {
                winner = String.valueOf(board[i][0]);
                status = GameStatus.FINISHED;
                return;
            }
        }

        // Check columns
        for (int i = 0; i < 3; i++) {
            if (board[0][i] != ' ' && board[0][i] == board[1][i] && board[1][i] == board[2][i]) {
                winner = String.valueOf(board[0][i]);
                status = GameStatus.FINISHED;
                return;
            }
        }

        // Check diagonals
        if (board[0][0] != ' ' && board[0][0] == board[1][1] && board[1][1] == board[2][2]) {
            winner = String.valueOf(board[0][0]);
            status = GameStatus.FINISHED;
            return;
        }

        if (board[0][2] != ' ' && board[0][2] == board[1][1] && board[1][1] == board[2][0]) {
            winner = String.valueOf(board[0][2]);
            status = GameStatus.FINISHED;
            return;
        }

        // Check for draw
        boolean boardFull = true;
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    boardFull = false;
                    break;
                }
            }
        }

        if (boardFull) {
            winner = "DRAW";
            status = GameStatus.FINISHED;
        }
    }

    public boolean isFull() {
        return player1 != null && player2 != null;
    }

    public String getPlayerSymbol(String nickname) {
        if (player1 != null && player1.getNickname().equals(nickname)) {
            return "X";
        } else if (player2 != null && player2.getNickname().equals(nickname)) {
            return "O";
        }
        return null;
    }
}