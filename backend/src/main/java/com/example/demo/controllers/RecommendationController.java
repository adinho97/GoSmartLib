package com.example.demo.controllers;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.services.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/aanbevelingen")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    public ResponseEntity<List<RecommendedBook>> getRecommendations(
            Authentication authentication,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "excludeRead", defaultValue = "true") boolean excludeRead) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        try {
            var recommendations = recommendationService.getRecommendations(userSub, limit, excludeRead);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-strategy")
    public ResponseEntity<List<RecommendedBook>> getRecommendationsByStrategy(
            Authentication authentication,
            @RequestParam(value = "strategies") String strategies,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "excludeRead", defaultValue = "true") boolean excludeRead) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        try {
            var strategyList = List.of(strategies.split(","));
            var recommendations = recommendationService.getRecommendationsByStrategy(userSub, strategyList, limit,
                    excludeRead);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<RecommendedBook>>> getGroupedRecommendations(
            Authentication authentication,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "excludeRead", defaultValue = "true") boolean excludeRead) {

        if (authentication == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String userSub = authentication.getName();
        try {
            var recommendations = recommendationService.getRecommendationsByStrategyGrouped(userSub, limit,
                    excludeRead);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/strategies")
    public ResponseEntity<List<String>> getAvailableStrategies() {
        var strategies = recommendationService.getAvailableStrategies();
        return ResponseEntity.ok(strategies);
    }
}
