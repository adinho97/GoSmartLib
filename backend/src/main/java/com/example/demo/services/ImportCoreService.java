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
    private final BookMapper bookMapper;

    public ImportCoreService(BookRepository bookRepository,
            OpenLibraryService openLibraryService, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.openLibraryService = openLibraryService;
        this.bookMapper = bookMapper;
    }

    public ImportOutcome importByNormalizedIsbn(String normalizedIsbn, School school) {
        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(normalizedIsbn, school.getId());
        if (existing.isPresent()) {
            return new ImportOutcome(ImportStatus.ALREADY_EXISTS, bookMapper.toDto(existing.get()));
        }

        Book fetched = openLibraryService.fetchBookFromOpenLibrary(normalizedIsbn);
        if (fetched == null) {
            return new ImportOutcome(ImportStatus.NOT_FOUND, null);
        }

        fetched.setIsbn(normalizedIsbn);
        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return new ImportOutcome(ImportStatus.ADDED, bookMapper.toDto(saved));
    }

    public enum ImportStatus {
        ADDED,
        ALREADY_EXISTS,
        NOT_FOUND
    }

    public record ImportOutcome(ImportStatus status, BookDto bookDto) {
    }
}
