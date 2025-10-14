package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JoinGameRequest {
    private String nickname;

    public void setNickname(String nickname) {
        this.nickname = nickname != null ? nickname.trim() : null;
    }
}
