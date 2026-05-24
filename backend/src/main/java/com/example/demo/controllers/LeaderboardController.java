package com.example.demo.controllers;

import com.example.demo.dto.LeaderboardResponseDTO;
import com.example.demo.services.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping
    public ResponseEntity<LeaderboardResponseDTO> getLeaderboard(
            Authentication authentication,
            @RequestParam(required = false) Long klasId) {
        LeaderboardResponseDTO response = leaderboardService.getLeaderboardData(authentication.getName(), klasId);
        return ResponseEntity.ok(response);
    }
}