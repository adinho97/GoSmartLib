package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class BookLookupService {

    private final BookRepository bookRepository;
    private final IsbnService isbnService;
    private final OpenLibraryService openLibraryService;
    private final BookMapper bookMapper;

    public BookLookupService(BookRepository bookRepository,
            IsbnService isbnService,
            OpenLibraryService openLibraryService,
            BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.isbnService = isbnService;
        this.openLibraryService = openLibraryService;
        this.bookMapper = bookMapper;
    }

    public Optional<BookDto> findByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);

        if (schoolId == null) {
            return bookRepository.findByIsbn(normalizedIsbn)
                    .map(bookMapper::toDto);
        }

        return bookRepository.findByIsbnAndSchool_Id(normalizedIsbn, schoolId)
                .map(bookMapper::toDto);
    }

    public BookDto fetchPreviewByIsbn(String isbn) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);
        Book fetched = openLibraryService.fetchBookFromOpenLibrary(normalizedIsbn);
        if (fetched == null) {
            return null;
        }
        return bookMapper.toDto(fetched);
    }
}
