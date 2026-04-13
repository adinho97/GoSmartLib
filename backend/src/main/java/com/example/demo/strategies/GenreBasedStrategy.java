package com.example.demo.strategies;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class GenreBasedStrategy implements RecommendationStrategy {

    private final AppUserRepository appUserRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public GenreBasedStrategy(AppUserRepository appUserRepository,
            FavoriteRepository favoriteRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.appUserRepository = appUserRepository;
        this.favoriteRepository = favoriteRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit) {
        Optional<AppUser> userOpt = appUserRepository.findBySub(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        AppUser user = userOpt.get();

        var favorites = favoriteRepository.findByUser(user);
        if (favorites.isEmpty()) {
            return List.of(); // No favorites = no recommendations yet
        }

        // Extract genres from favorites and count frequency
        Map<String, Integer> genreFrequency = new HashMap<>();
        for (var favorite : favorites) {
            String genre = favorite.getBook().getGenre();
            if (genre != null && !genre.isBlank()) {
                genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0) + 1);
            }
        }

        if (genreFrequency.isEmpty()) {
            return List.of(); // User has favorites but no genres set
        }

        // Get IDs of books user already has (favorites + loans)
        Set<Long> userBookIds = new HashSet<>();
        for (var favorite : favorites) {
            userBookIds.add(favorite.getBook().getId());
        }
        var loans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
        for (var loan : loans) {
            userBookIds.add(loan.getCopy().getBook().getId());
        }

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = bookRepository.findAll();

        for (Book book : allBooks) {
            // Skip if user already has it
            if (userBookIds.contains(book.getId())) {
                continue;
            }

            // Skip if book has no genre
            if (book.getGenre() == null || book.getGenre().isBlank()) {
                continue;
            }

            // Score: how often this genre appears in user's favorites
            Integer genreCount = genreFrequency.get(book.getGenre());
            if (genreCount != null) {
                double score = (genreCount.doubleValue() / favorites.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Matches your favorite genre: " + book.getGenre());
                scored.add(rec);
            }
        }

        // Sort by score descending and limit
        return scored.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit, boolean excludeRead) {
        Optional<AppUser> userOpt = appUserRepository.findBySub(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        AppUser user = userOpt.get();

        var favorites = favoriteRepository.findByUser(user);
        if (favorites.isEmpty()) {
            return List.of();
        }

        // Extract genres from favorites and count frequency
        Map<String, Integer> genreFrequency = new HashMap<>();
        for (var favorite : favorites) {
            String genre = favorite.getBook().getGenre();
            if (genre != null && !genre.isBlank()) {
                genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0) + 1);
            }
        }

        if (genreFrequency.isEmpty()) {
            return List.of();
        }

        // Get IDs of books user already has (only if excludeRead is true)
        Set<Long> userBookIds = new HashSet<>();
        if (excludeRead) {
            for (var favorite : favorites) {
                userBookIds.add(favorite.getBook().getId());
            }
            var loans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
            for (var loan : loans) {
                userBookIds.add(loan.getCopy().getBook().getId());
            }
        }

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = bookRepository.findAll();

        for (Book book : allBooks) {
            // Skip if user already has it (only if excludeRead is true)
            if (excludeRead && userBookIds.contains(book.getId())) {
                continue;
            }

            // Skip if book has no genre
            if (book.getGenre() == null || book.getGenre().isBlank()) {
                continue;
            }

            // Score: how often this genre appears in user's favorites
            Integer genreCount = genreFrequency.get(book.getGenre());
            if (genreCount != null) {
                double score = (genreCount.doubleValue() / favorites.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Matches your favorite genre: " + book.getGenre());
                scored.add(rec);
            }
        }

        // Sort by score descending and limit
        return scored.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .limit(limit)
                .collect(Collectors.toList());
    }
}
