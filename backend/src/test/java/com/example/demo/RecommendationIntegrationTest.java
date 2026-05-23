package com.example.demo;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Genre;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.services.RecommendationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Recommendation Integration Tests")
class RecommendationIntegrationTest {

    @Autowired
    private RecommendationService recommendationService;

    @Autowired
    private AppUserRepository appUserRepository;

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private LoanRepository loanRepository;

    private AppUser testUser;

    @BeforeEach
    void setUp() {
        loanRepository.deleteAll();
        bookRepository.deleteAll();
        appUserRepository.deleteAll();

        testUser = new AppUser();
        testUser.setSub("integration_user");
        testUser.setRole("leerling");
        appUserRepository.save(testUser);

        createAndSaveBook(1L, "Fantasy Book", "Author A", "Fantasy");
        createAndSaveBook(2L, "Another Fantasy", "Author B", "Fantasy");
        createAndSaveBook(3L, "Other by Author A", "Author A", "SciFi");
        createAndSaveBook(4L, "Brand New Book", "Author C", "Mystery");
    }

    @Test
    @DisplayName("should combine recommendations from multiple strategies")
    void testMultipleStrategiesIntegration() {
        List<RecommendedBook> combined = recommendationService.getRecommendations(
                "integration_user", 10, true);

        assertNotNull(combined);
        assertTrue(combined.size() > 0);
    }

    @Test
    @DisplayName("should return grouped recommendations with all strategies")
    void testGroupedRecommendationsAllStrategies() {
        Map<String, List<RecommendedBook>> grouped = recommendationService.getRecommendationsByStrategyGrouped(
                "integration_user", 10, true);

        assertTrue(grouped.containsKey("TrendingStrategy"));
        assertTrue(grouped.containsKey("GenreBasedStrategy"));
        assertTrue(grouped.containsKey("AuthorBasedStrategy"));
        assertTrue(grouped.containsKey("NewArrivalsStrategy"));
    }

    @Test
    @DisplayName("should list all available strategies")
    void testAvailableStrategies() {
        List<String> strategies = recommendationService.getAvailableStrategies();

        assertEquals(4, strategies.size());
        assertTrue(strategies.contains("TrendingStrategy"));
        assertTrue(strategies.contains("GenreBasedStrategy"));
        assertTrue(strategies.contains("AuthorBasedStrategy"));
        assertTrue(strategies.contains("NewArrivalsStrategy"));
    }

    @Test
    @DisplayName("should filter recommendations by strategy names")
    void testFilterByStrategyNames() {
        List<RecommendedBook> filtered = recommendationService.getRecommendationsByStrategy(
                "integration_user",
                List.of("GenreBasedStrategy"),
                10,
                true);

        assertNotNull(filtered);
    }

    private Book createAndSaveBook(Long id, String title, String author, String genre) {
        Book book = new Book();
        book.setTitel(title);
        book.setAuteur(author);
        // Create a Genre object and add it to a Set
        Genre newGenre = new Genre();
        newGenre.setNaam(genre);
        book.setGenres(Set.of(newGenre));
        return bookRepository.save(book);
    }
}
