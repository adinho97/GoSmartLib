package com.example.demo;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.services.RecommendationService;
import com.example.demo.strategies.RecommendationStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationService Tests")
class RecommendationServiceTest {

    @Mock
    private RecommendationStrategy strategy1;

    @Mock
    private RecommendationStrategy strategy2;

    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationService = new RecommendationService(Arrays.asList(strategy1, strategy2));
    }

    @Test
    @DisplayName("should average scores when book appears in multiple strategies")
    void testScoreAveragingWhenBookInMultipleStrategies() {
        RecommendedBook book1From1 = new RecommendedBook(1L, "Book 1", "Author", List.of("Genre"), 80.0, "Reason 1");
        RecommendedBook book2From1 = new RecommendedBook(2L, "Book 2", "Author", List.of("Genre"), 60.0, "Reason 1");

        RecommendedBook book1From2 = new RecommendedBook(1L, "Book 1", "Author", List.of("Genre"), 100.0, "Reason 2");
        RecommendedBook book3From2 = new RecommendedBook(3L, "Book 3", "Author", List.of("Genre"), 70.0, "Reason 2");

        when(strategy1.recommend("user123", 20, true))
                .thenReturn(List.of(book1From1, book2From1));
        when(strategy2.recommend("user123", 20, true))
                .thenReturn(List.of(book1From2, book3From2));

        List<RecommendedBook> result = recommendationService.getRecommendations("user123", 10, true);

        RecommendedBook book1Result = result.stream()
                .filter(b -> b.getBookId() == 1L)
                .findFirst()
                .orElse(null);

        assertNotNull(book1Result);
        assertEquals(90.0, book1Result.getScore(), 0.01);
        assertTrue(book1Result.getReason().contains("Reason 1"));
        assertTrue(book1Result.getReason().contains("Reason 2"));
    }

    @Test
    @DisplayName("should concatenate reasons with pipe separator")
    void testReasonConcatenation() {
        RecommendedBook book1From1 = new RecommendedBook(1L, "Book 1", "Author", List.of("Genre"), 80.0, "Popular");
        RecommendedBook book1From2 = new RecommendedBook(1L, "Book 1", "Author", List.of("Genre"), 60.0, "Your Genre");

        when(strategy1.recommend("user123", 20, true)).thenReturn(List.of(book1From1));
        when(strategy2.recommend("user123", 20, true)).thenReturn(List.of(book1From2));

        List<RecommendedBook> result = recommendationService.getRecommendations("user123", 10, true);

        assertEquals(1, result.size());
        assertEquals("Popular | Your Genre", result.get(0).getReason());
    }

    @Test
    @DisplayName("should respect limit parameter")
    void testLimitRespected() {
        List<RecommendedBook> strategy1Results = List.of(
                new RecommendedBook(1L, "B1", "A", List.of("G"), 100.0, "R1"),
                new RecommendedBook(2L, "B2", "A", List.of("G"), 90.0, "R1"),
                new RecommendedBook(3L, "B3", "A", List.of("G"), 80.0, "R1"));
        List<RecommendedBook> strategy2Results = new ArrayList<>();

        when(strategy1.recommend("user123", 4, true)).thenReturn(strategy1Results);
        when(strategy2.recommend("user123", 4, true)).thenReturn(strategy2Results);

        List<RecommendedBook> result = recommendationService.getRecommendations("user123", 2, true);

        assertEquals(2, result.size());
    }

    @Test
    @DisplayName("should filter by strategy name in getRecommendationsByStrategy")
    void testGetRecommendationsByStrategyName() {
        when(strategy1.getName()).thenReturn("TrendingStrategy");
        when(strategy2.getName()).thenReturn("GenreBasedStrategy");

        RecommendedBook book1 = new RecommendedBook(1L, "B1", "A", List.of("G"), 100.0, "R1");

        when(strategy1.recommend("user123", 20, true)).thenReturn(List.of(book1));

        List<RecommendedBook> result = recommendationService.getRecommendationsByStrategy(
                "user123",
                List.of("TrendingStrategy"),
                10,
                true);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getBookId());
    }

    @Test
    @DisplayName("should return grouped recommendations by strategy")
    void testGetRecommendationsByStrategyGrouped() {
        when(strategy1.getName()).thenReturn("TrendingStrategy");
        when(strategy2.getName()).thenReturn("GenreBasedStrategy");

        RecommendedBook book1 = new RecommendedBook(1L, "B1", "A", List.of("G"), 100.0, "R1");
        RecommendedBook book2 = new RecommendedBook(2L, "B2", "A", List.of("G"), 90.0, "R2");

        when(strategy1.recommend("user123", 10, true)).thenReturn(List.of(book1));
        when(strategy2.recommend("user123", 10, true)).thenReturn(List.of(book2));

        Map<String, List<RecommendedBook>> result = recommendationService.getRecommendationsByStrategyGrouped(
                "user123",
                10,
                true);

        assertEquals(2, result.size());
        assertTrue(result.containsKey("TrendingStrategy"));
        assertTrue(result.containsKey("GenreBasedStrategy"));
        assertEquals(1, result.get("TrendingStrategy").size());
        assertEquals(1, result.get("GenreBasedStrategy").size());
    }

    @Test
    @DisplayName("should return list of available strategies")
    void testGetAvailableStrategies() {
        when(strategy1.getName()).thenReturn("TrendingStrategy");
        when(strategy2.getName()).thenReturn("GenreBasedStrategy");

        List<String> result = recommendationService.getAvailableStrategies();

        assertEquals(2, result.size());
        assertTrue(result.contains("TrendingStrategy"));
        assertTrue(result.contains("GenreBasedStrategy"));
    }

    @Test
    @DisplayName("should sort results by score descending")
    void testResultsSortedByScoreDescending() {
        RecommendedBook book1 = new RecommendedBook(1L, "B1", "A", List.of("G"), 50.0, "R1");
        RecommendedBook book2 = new RecommendedBook(2L, "B2", "A", List.of("G"), 100.0, "R2");
        RecommendedBook book3 = new RecommendedBook(3L, "B3", "A", List.of("G"), 75.0, "R3");

        when(strategy1.recommend("user123", 20, true)).thenReturn(List.of(book1, book2, book3));
        when(strategy2.recommend("user123", 20, true)).thenReturn(new ArrayList<>());

        List<RecommendedBook> result = recommendationService.getRecommendations("user123", 10, true);

        assertEquals(100.0, result.get(0).getScore());
        assertEquals(75.0, result.get(1).getScore());
        assertEquals(50.0, result.get(2).getScore());
    }

    @Test
    @DisplayName("should handle excludeRead parameter passing to strategies")
    void testExcludeReadParameterPassed() {
        when(strategy1.recommend("user123", 20, false)).thenReturn(new ArrayList<>());
        when(strategy2.recommend("user123", 20, false)).thenReturn(new ArrayList<>());

        recommendationService.getRecommendations("user123", 10, false);

        verify(strategy1).recommend("user123", 20, false);
        verify(strategy2).recommend("user123", 20, false);
    }
}
