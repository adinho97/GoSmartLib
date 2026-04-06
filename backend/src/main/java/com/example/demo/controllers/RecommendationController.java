package com.example.demo.controllers;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.services.RecommendationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var recommendations = recommendationService.getRecommendations(userSub, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/by-strategy")
    public ResponseEntity<List<RecommendedBook>> getRecommendationsByStrategy(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestParam(value = "strategies") String strategies,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var strategyList = List.of(strategies.split(","));
            var recommendations = recommendationService.getRecommendationsByStrategy(userSub, strategyList, limit);
            return ResponseEntity.ok(recommendations);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/grouped")
    public ResponseEntity<Map<String, List<RecommendedBook>>> getGroupedRecommendations(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestParam(value = "limit", defaultValue = "10") int limit) {

        if (userSub == null || userSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        try {
            var recommendations = recommendationService.getRecommendationsByStrategyGrouped(userSub, limit);
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
