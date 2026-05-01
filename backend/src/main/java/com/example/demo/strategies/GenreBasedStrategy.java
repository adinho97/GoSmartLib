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
public class GenreBasedStrategy implements RecommendationStrategy {

    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public GenreBasedStrategy(AppUserRepository appUserRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.appUserRepository = appUserRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    private List<Book> getBooksForUser(AppUser user) {
        if (user.getSchool() != null) {
            return bookRepository.findAllBySchool_Id(user.getSchool().getId());
        }
        return bookRepository.findAll();
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

        // Extract genres from loans and count frequency
        Map<String, Integer> genreFrequency = new HashMap<>();
        for (var loan : loans) {
            String genre = loan.getCopy().getBook().getGenre();
            if (genre != null && !genre.isBlank()) {
                genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0) + 1);
            }
        }

        if (genreFrequency.isEmpty()) {
            return List.of();
        }

        // Get IDs of books user already has
        Set<Long> userBookIds = new HashSet<>();
        loans.forEach(loan -> userBookIds.add(loan.getCopy().getBook().getId()));

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = getBooksForUser(user);

        for (Book book : allBooks) {
            // Skip if user already has it
            if (userBookIds.contains(book.getId())) {
                continue;
            }

            // Skip if book has no genre
            if (book.getGenre() == null || book.getGenre().isBlank()) {
                continue;
            }

            // Score: how often this genre appears in user's loan history
            Integer genreCount = genreFrequency.get(book.getGenre());
            if (genreCount != null) {
                double score = (genreCount.doubleValue() / loans.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Matches your reading history: " + book.getGenre());
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

        // Extract genres from loans and count frequency
        Map<String, Integer> genreFrequency = new HashMap<>();
        for (var loan : loans) {
            String genre = loan.getCopy().getBook().getGenre();
            if (genre != null && !genre.isBlank()) {
                genreFrequency.put(genre, genreFrequency.getOrDefault(genre, 0) + 1);
            }
        }

        if (genreFrequency.isEmpty()) {
            return List.of();
        }

        Set<Long> userBookIds = new HashSet<>();
        loans.forEach(loan -> userBookIds.add(loan.getCopy().getBook().getId()));

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = getBooksForUser(user);

        for (Book book : allBooks) {
            // Skip if user already has it (only if excludeRead is true)
            if (excludeRead && userBookIds.contains(book.getId())) {
                continue;
            }

            // Skip if book has no genre
            if (book.getGenre() == null || book.getGenre().isBlank()) {
                continue;
            }

            // Score: how often this genre appears in user's loan history
            Integer genreCount = genreFrequency.get(book.getGenre());
            if (genreCount != null) {
                double score = (genreCount.doubleValue() / loans.size()) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Matches your reading history: " + book.getGenre());
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
