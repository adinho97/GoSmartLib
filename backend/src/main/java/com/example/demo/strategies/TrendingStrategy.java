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
public class TrendingStrategy implements RecommendationStrategy {

    private final AppUserRepository appUserRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public TrendingStrategy(AppUserRepository appUserRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.appUserRepository = appUserRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    private List<Book> getBooksForUser(AppUser user) {
        if (user.getSchool() == null) return bookRepository.findAll();
        Long schoolId = user.getSchool().getId();
        if ("leerling".equalsIgnoreCase(user.getRole())) {
            return bookRepository.findNonDidacticBySchool_Id(schoolId);
        }
        return bookRepository.findAllBySchool_Id(schoolId);
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit) {
        Optional<AppUser> userOpt = appUserRepository.findBySub(userId);
        if (userOpt.isEmpty()) {
            return List.of();
        }
        AppUser user = userOpt.get();

        // Get IDs of books user already has
        var userLoans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
        Set<Long> userBookIds = userLoans.stream().map(l -> l.getCopy().getBook().getId()).collect(Collectors.toSet());

        // Count loan frequency across ALL users
        Map<Long, Integer> loanCounts = new HashMap<>();
        var allLoans = loanRepository.findAll();
        for (var loan : allLoans) {
            Long bookId = loan.getCopy().getBook().getId();
            loanCounts.put(bookId, loanCounts.getOrDefault(bookId, 0) + 1);
        }

        if (loanCounts.isEmpty()) {
            return List.of(); // No loans in system yet
        }

        // Find max loan count for scoring normalization
        int maxLoans = loanCounts.values().stream()
                .max(Integer::compareTo)
                .orElse(1);

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = getBooksForUser(user);

        for (Book book : allBooks) {
            // Skip if user already has it
            if (userBookIds.contains(book.getId())) {
                continue;
            }

            // Score based on loan frequency (normalized to 0-100)
            Integer loanCount = loanCounts.getOrDefault(book.getId(), 0);
            if (loanCount > 0) {
                double score = (loanCount.doubleValue() / maxLoans) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Popular in our library (" + loanCount + " loans)");
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

        Set<Long> userBookIds = new HashSet<>();
        if (excludeRead) {
            var userLoans = loanRepository.findByUserSubAndReturnedAtIsNotNull(userId);
            userLoans.forEach(loan -> userBookIds.add(loan.getCopy().getBook().getId()));
        }

        // Count loan frequency across ALL users
        Map<Long, Integer> loanCounts = new HashMap<>();
        var allLoans = loanRepository.findAll();
        for (var loan : allLoans) {
            Long bookId = loan.getCopy().getBook().getId();
            loanCounts.put(bookId, loanCounts.getOrDefault(bookId, 0) + 1);
        }

        if (loanCounts.isEmpty()) {
            return List.of();
        }

        // Find max loan count for scoring normalization
        int maxLoans = loanCounts.values().stream()
                .max(Integer::compareTo)
                .orElse(1);

        // Score available books
        List<RecommendedBook> scored = new ArrayList<>();
        var allBooks = getBooksForUser(user);

        for (Book book : allBooks) {
            // Skip if user already has it (only if excludeRead is true)
            if (excludeRead && userBookIds.contains(book.getId())) {
                continue;
            }

            // Score based on loan frequency (normalized to 0-100)
            Integer loanCount = loanCounts.getOrDefault(book.getId(), 0);
            if (loanCount > 0) {
                double score = (loanCount.doubleValue() / maxLoans) * 100;
                RecommendedBook rec = new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        score,
                        "Popular in our library (" + loanCount + " loans)");
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
    public String getName() {
        return "TrendingStrategy";
    }
}
