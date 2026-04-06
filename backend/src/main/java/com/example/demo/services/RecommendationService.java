package com.example.demo.services;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.strategies.RecommendationStrategy;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationService {

    private final List<RecommendationStrategy> strategies;

    public RecommendationService(List<RecommendationStrategy> strategies) {
        this.strategies = strategies;
    }

    public List<RecommendedBook> getRecommendations(String userId, int limit) {
        // Run all strategies and collect results
        Map<Long, RecommendedBook> combined = new HashMap<>();

        for (RecommendationStrategy strategy : strategies) {
            var result = strategy.recommend(userId, limit * 2); // Get more than needed per strategy

            for (RecommendedBook book : result) {
                if (combined.containsKey(book.getBookId())) {
                    // Book appears in multiple strategies - average the scores
                    RecommendedBook existing = combined.get(book.getBookId());
                    double newScore = (existing.getScore() + book.getScore()) / 2;
                    existing.setScore(newScore);
                    existing.setReason(existing.getReason() + " | " + book.getReason());
                } else {
                    combined.put(book.getBookId(), book);
                }
            }
        }

        return combined.values().stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<RecommendedBook> getRecommendationsByStrategy(String userId, List<String> strategyNames, int limit) {
        Map<Long, RecommendedBook> combined = new HashMap<>();

        for (RecommendationStrategy strategy : strategies) {
            // Only run if this strategy is in the requested list
            if (strategyNames.contains(strategy.getClass().getSimpleName())) {
                var result = strategy.recommend(userId, limit * 2);

                for (RecommendedBook book : result) {
                    if (combined.containsKey(book.getBookId())) {
                        RecommendedBook existing = combined.get(book.getBookId());
                        double newScore = (existing.getScore() + book.getScore()) / 2;
                        existing.setScore(newScore);
                        existing.setReason(existing.getReason() + " | " + book.getReason());
                    } else {
                        combined.put(book.getBookId(), book);
                    }
                }
            }
        }

        return combined.values().stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    public Map<String, List<RecommendedBook>> getRecommendationsByStrategyGrouped(String userId, int limit) {
        Map<String, List<RecommendedBook>> grouped = new LinkedHashMap<>();

        for (RecommendationStrategy strategy : strategies) {
            var results = strategy.recommend(userId, limit);
            grouped.put(strategy.getClass().getSimpleName(), results);
        }

        return grouped;
    }

    // Useful for clients to know what strategies are active.
    public List<String> getAvailableStrategies() {
        return strategies.stream()
                .map(s -> s.getClass().getSimpleName())
                .collect(Collectors.toList());
    }
}
