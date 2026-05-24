package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class BookStatsService {

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final BookMapper bookMapper;

    public BookStatsService(BookRepository bookRepository,
            LoanRepository loanRepository,
            BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
        this.bookMapper = bookMapper;
    }

    public List<BookDto> getBooksWithStats() {
        List<Book> books = bookRepository.findAll();
        Map<Long, Long> loanCountMap = buildLoanCountMap();

        return books.stream()
                .map(bookMapper::toDto)
                .peek(dto -> dto.setLoanCount(loanCountMap.getOrDefault(dto.getId(), 0L)))
                .sorted((a, b) -> Long.compare(b.getLoanCount(), a.getLoanCount()))
                .collect(Collectors.toList());
    }

    private Map<Long, Long> buildLoanCountMap() {
        Map<Long, Long> loanCountMap = new HashMap<>();
        List<Map<String, Object>> loanStats = loanRepository.getLoanCountsByBook();

        for (Map<String, Object> stat : loanStats) {
            Long bookId = ((Number) stat.get("bookId")).longValue();
            Long count = ((Number) stat.get("loanCount")).longValue();
            loanCountMap.put(bookId, count);
        }

        return loanCountMap;
    }
}
