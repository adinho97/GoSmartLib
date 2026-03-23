package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ImportCoreService {

    private final BookRepository bookRepository;
    private final OpenLibraryService openLibraryService;
    private final IsbnService isbnService;

    public ImportCoreService(BookRepository bookRepository,
            OpenLibraryService openLibraryService,
            IsbnService isbnService) {
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
        this.isbnService = isbnService;
    }

    public ImportOutcome importByNormalizedIsbn(String normalizedIsbn, School school) {
        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(normalizedIsbn, school.getId());
        if (existing.isPresent()) {
            return new ImportOutcome(ImportStatus.ALREADY_EXISTS, BookMapper.toDto(existing.get()));
        }

        Book fetched = openLibraryService.fetchBookFromOpenLibrary(normalizedIsbn);
        if (fetched == null) {
            return new ImportOutcome(ImportStatus.NOT_FOUND, null);
        }

        fetched.setIsbn(isbnService.normalizeAndValidateIsbn(fetched.getIsbn())
                .orElse(fetched.getIsbn()));

        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return new ImportOutcome(ImportStatus.ADDED, BookMapper.toDto(saved));
    }

    public enum ImportStatus {
        ADDED,
        ALREADY_EXISTS,
        NOT_FOUND
    }

    public record ImportOutcome(ImportStatus status, BookDto bookDto) {
    }
}
