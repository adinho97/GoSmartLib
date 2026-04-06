package com.example.demo.strategies;

import com.example.demo.dto.RecommendedBook;
import java.util.List;

/**
 * Strategy interface for generating book recommendations.
 * Implementations provide different algorithms (genre-based, author-based,
 * collaborative, etc.)
 */
public interface RecommendationStrategy {

    List<RecommendedBook> recommend(String userId, int limit);
    
    List<RecommendedBook> recommend(String userId, int limit, boolean excludeRead);
}
