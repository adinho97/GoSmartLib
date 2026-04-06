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
public class AuthorBasedStrategy implements RecommendationStrategy {

    private final AppUserRepository appUserRepository;
    private final FavoriteRepository favoriteRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public AuthorBasedStrategy(AppUserRepository appUserRepository,
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

        // Extract authors from favorites and count frequency
        Map<String, Integer> authorFrequency = new HashMap<>();
        for (var favorite : favorites) {
            String author = favorite.getBook().getAuteur();
            if (author != null && !author.isBlank()) {
                authorFrequency.put(author, authorFrequency.getOrDefault(author, 0) + 1);
            }
        }

        if (authorFrequency.isEmpty()) {
            return List.of(); // User has favorites but no authors set
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

            // Skip if book has no author
            if (book.getAuteur() == null || book.getAuteur().isBlank()) {
                continue;
            }

            // Score: how often this author appears in user's favorites
            Integer authorCount = authorFrequency.get(book.getAuteur());
            if (authorCount != null) {
                double score = (authorCount.doubleValue() / favorites.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "By your favorite author: " + book.getAuteur());
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

        // Extract authors from favorites and count frequency
        Map<String, Integer> authorFrequency = new HashMap<>();
        for (var favorite : favorites) {
            String author = favorite.getBook().getAuteur();
            if (author != null && !author.isBlank()) {
                authorFrequency.put(author, authorFrequency.getOrDefault(author, 0) + 1);
            }
        }

        if (authorFrequency.isEmpty()) {
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

            // Skip if book has no author
            if (book.getAuteur() == null || book.getAuteur().isBlank()) {
                continue;
            }

            // Score: how often this author appears in user's favorites
            Integer authorCount = authorFrequency.get(book.getAuteur());
            if (authorCount != null) {
                double score = (authorCount.doubleValue() / favorites.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "By your favorite author: " + book.getAuteur());
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
