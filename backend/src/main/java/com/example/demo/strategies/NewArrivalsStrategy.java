package com.example.demo.strategies;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.Book;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.entities.Genre;
import com.example.demo.repositories.BookRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewArrivalsStrategy implements RecommendationStrategy {

    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;

    public NewArrivalsStrategy(BookRepository bookRepository, AppUserRepository appUserRepository) {
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit) {
        return recommend(userId, limit, true);
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit, boolean excludeRead) {
        List<Book> books = getBooksForUser(userId);

        return books.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId()))
                .limit(limit)
                .map(book -> new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenres() != null
                            ? book.getGenres().stream().map(Genre::getNaam).collect(Collectors.joining(", "))
                            : "",
                        100.0,
                        "Recently added to the library"))
                .collect(Collectors.toList());
    }

    @Override
    public String getName() {
        return "NewArrivalsStrategy";
    }

    private List<Book> getBooksForUser(String userId) {
        if (userId == null || userId.isBlank()) {
            return bookRepository.findAll();
        }
        return appUserRepository.findBySub(userId.trim())
                .map(user -> {
                    if (user.getSchool() == null) return bookRepository.findAll();
                    Long schoolId = user.getSchool().getId();
                    if ("leerling".equalsIgnoreCase(user.getRole())) {
                        return bookRepository.findNonDidacticBySchool_Id(schoolId);
                    }
                    return bookRepository.findAllBySchool_Id(schoolId);
                })
                .orElseGet(bookRepository::findAll);
    }
}
