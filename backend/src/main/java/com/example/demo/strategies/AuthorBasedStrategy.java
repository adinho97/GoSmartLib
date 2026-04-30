package com.example.demo.strategies;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class AuthorBasedStrategy implements RecommendationStrategy {

    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public AuthorBasedStrategy(AppUserRepository appUserRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.appUserRepository = appUserRepository;
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

        var loans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
        if (loans.isEmpty()) {
            return List.of();
        }

        // Extract authors from loans and count frequency
        Map<String, Integer> authorFrequency = new HashMap<>();
        for (var loan : loans) {
            String author = loan.getCopy().getBook().getAuteur();
            if (author != null && !author.isBlank()) {
                authorFrequency.put(author, authorFrequency.getOrDefault(author, 0) + 1);
            }
        }

        if (authorFrequency.isEmpty()) {
            return List.of();
        }

        // Get IDs of books user already has
        Set<Long> userBookIds = new HashSet<>();
        loans.forEach(loan -> userBookIds.add(loan.getCopy().getBook().getId()));

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

            // Score: how often this author appears in user's loan history
            Integer authorCount = authorFrequency.get(book.getAuteur());
            if (authorCount != null) {
                double score = (authorCount.doubleValue() / loans.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "By an author you read before: " + book.getAuteur());
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

        var loans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
        if (loans.isEmpty()) {
            return List.of();
        }

        // Extract authors from loans and count frequency
        Map<String, Integer> authorFrequency = new HashMap<>();
        for (var loan : loans) {
            String author = loan.getCopy().getBook().getAuteur();
            if (author != null && !author.isBlank()) {
                authorFrequency.put(author, authorFrequency.getOrDefault(author, 0) + 1);
            }
        }

        if (authorFrequency.isEmpty()) {
            return List.of();
        }

        Set<Long> userBookIds = new HashSet<>();
        loans.forEach(loan -> userBookIds.add(loan.getCopy().getBook().getId()));

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

            // Score: how often this author appears in user's loan history
            Integer authorCount = authorFrequency.get(book.getAuteur());
            if (authorCount != null) {
                double score = (authorCount.doubleValue() / loans.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "By an author you read before: " + book.getAuteur());
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
