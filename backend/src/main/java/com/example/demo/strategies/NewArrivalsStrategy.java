package com.example.demo.strategies;

import com.example.demo.dto.RecommendedBook;
import com.example.demo.entities.Book;
import com.example.demo.repositories.BookRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class NewArrivalsStrategy implements RecommendationStrategy {

    private final BookRepository bookRepository;

    public NewArrivalsStrategy(BookRepository bookRepository) {
        this.bookRepository = bookRepository;
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit) {
        return recommend(userId, limit, true);
    }

    @Override
    public List<RecommendedBook> recommend(String userId, int limit, boolean excludeRead) {
        // New Arrivals doesn't need user data - always returns recently added books
        List<Book> allBooks = bookRepository.findAll();

        return allBooks.stream()
                .sorted((a, b) -> b.getId().compareTo(a.getId())) // Descending order (newest first)
                .limit(limit)
                .map(book -> new RecommendedBook(
                        book.getId(),
                        book.getTitel(),
                        book.getAuteur(),
                        book.getGenre(),
                        100.0, // All new arrivals have max score
                        "Recently added to the library"
                ))
                .collect(Collectors.toList());
    }
}
