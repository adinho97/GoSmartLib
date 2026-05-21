package com.example.demo;

import com.example.demo.config.ConnectionPoolMonitor;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.controllers.RecommendationController;
import com.example.demo.dto.RecommendedBook;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.services.RecommendationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RecommendationController.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost")
@DisplayName("RecommendationController Tests")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecommendationService recommendationService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SuperAdminRepository superAdminRepository;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private ConnectionPoolMonitor connectionPoolMonitor;

    private static final String ENDPOINT = "/api/aanbevelingen";

    @Test
    @DisplayName("should return 401 when X-User-Sub header missing in getRecommendations")
    void testGetRecommendationsUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("should return recommendations with default limit 10")
    void testGetRecommendationsSuccess() throws Exception {
        List<RecommendedBook> mockRecommendations = List.of(
            new RecommendedBook(1L, "Book1", "Author1", List.of("Genre1"), 90.0, "Reason1"),
            new RecommendedBook(2L, "Book2", "Author2", List.of("Genre2"), 80.0, "Reason2")
        );

        when(recommendationService.getRecommendations("user123", 10, true))
            .thenReturn(mockRecommendations);

        mockMvc.perform(get(ENDPOINT)
            .header("X-User-Sub", "user123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(2))
            .andExpect(jsonPath("$[0].bookId").value(1))
            .andExpect(jsonPath("$[0].titel").value("Book1"))
            .andExpect(jsonPath("$[1].bookId").value(2));
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("should pass custom limit and excludeRead parameters")
    void testGetRecommendationsWithCustomParams() throws Exception {
        List<RecommendedBook> mockRecommendations = List.of(
            new RecommendedBook(1L, "Book1", "Author1", List.of("Genre1"), 90.0, "Reason1")
        );

        when(recommendationService.getRecommendations("user123", 5, false))
            .thenReturn(mockRecommendations);

        mockMvc.perform(get(ENDPOINT)
            .header("X-User-Sub", "user123")
            .param("limit", "5")
            .param("excludeRead", "false"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    @DisplayName("should return 401 in getByStrategy when X-User-Sub missing")
    void testGetByStrategyUnauthorized() throws Exception {
        mockMvc.perform(get(ENDPOINT + "/by-strategy")
            .param("strategies", "TrendingStrategy"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("should get recommendations filtered by strategy")
    void testGetByStrategySuccess() throws Exception {
        List<RecommendedBook> mockRecommendations = List.of(
            new RecommendedBook(1L, "Book1", "Author1", List.of("Genre1"), 100.0, "Trending")
        );

        when(recommendationService.getRecommendationsByStrategy(
            eq("user123"),
            argThat(list -> list.contains("TrendingStrategy")),
            eq(10),
            eq(true)))
            .thenReturn(mockRecommendations);

        mockMvc.perform(get(ENDPOINT + "/by-strategy")
            .header("X-User-Sub", "user123")
            .param("strategies", "TrendingStrategy"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].bookId").value(1));
    }

    @Test
    @WithMockUser(username = "user123")
    @DisplayName("should get grouped recommendations by strategy")
    void testGetGroupedRecommendationsSuccess() throws Exception {
        Map<String, List<RecommendedBook>> grouped = Map.of(
            "TrendingStrategy", List.of(
                new RecommendedBook(1L, "Book1", "Author1", List.of("Genre1"), 100.0, "Trending")
            ),
            "GenreBasedStrategy", List.of(
                new RecommendedBook(2L, "Book2", "Author2", List.of("Genre2"), 90.0, "Genre Based")
            )
        );

        when(recommendationService.getRecommendationsByStrategyGrouped("user123", 10, true))
            .thenReturn(grouped);

        mockMvc.perform(get(ENDPOINT + "/grouped")
            .header("X-User-Sub", "user123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.TrendingStrategy.length()").value(1))
            .andExpect(jsonPath("$.GenreBasedStrategy.length()").value(1));
    }

    @Test
    @WithMockUser
    @DisplayName("should return available strategies")
    void testGetAvailableStrategies() throws Exception {
        List<String> strategies = List.of(
            "TrendingStrategy",
            "GenreBasedStrategy",
            "AuthorBasedStrategy",
            "NewArrivalsStrategy"
        );

        when(recommendationService.getAvailableStrategies())
            .thenReturn(strategies);

        mockMvc.perform(get(ENDPOINT + "/strategies"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(4))
            .andExpect(jsonPath("$[0]").value("TrendingStrategy"))
            .andExpect(jsonPath("$[1]").value("GenreBasedStrategy"));
    }
}
