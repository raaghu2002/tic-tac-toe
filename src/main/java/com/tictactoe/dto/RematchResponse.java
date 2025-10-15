package com.tictactoe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RematchResponse {
    private String status; // "REQUESTED", "ACCEPTED", "DECLINED", "STARTED"
    private String gameId;
    private String message;
    private String requester;
}
