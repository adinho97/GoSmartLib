package com.example.demo.services;

import com.example.demo.repositories.BookRepository;
import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final SchoolService schoolService;
    private final OpenLibraryService openLibraryService;
    private final IsbnService isbnService;
    private final BulkImportService bulkImportService;

    public BookService(BookRepository bookRepository, SchoolService schoolService,
            OpenLibraryService openLibraryService, IsbnService isbnService, BulkImportService bulkImportService) {
        this.bookRepository = bookRepository;
        this.schoolService = schoolService;
        this.openLibraryService = openLibraryService;
        this.isbnService = isbnService;
        this.bulkImportService = bulkImportService;
    }

    public Optional<BookDto> findByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);

        if (schoolId == null) {
            return bookRepository.findByIsbn(normalizedIsbn)
                    .map(BookMapper::toDto);
        }

        return bookRepository.findByIsbnAndSchool_Id(normalizedIsbn, schoolId)
                .map(BookMapper::toDto);
    }

    public BookDto fetchPreviewByIsbn(String isbn) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);
        Book fetched = openLibraryService.fetchBookFromOpenLibrary(normalizedIsbn);
        if (fetched == null) {
            return null;
        }
        return BookMapper.toDto(fetched);
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);
        School school = schoolService.getByIdOrDefault(schoolId);
        ImportOutcome outcome = importByIsbnInternal(normalizedIsbn, school);
        return outcome.bookDto();
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId) {
        // Resolve school once; bulk uses the same school-scoped behavior as single ISBN
        // import.
        School school = schoolService.getByIdOrDefault(schoolId);
        BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.uniqueIsbns().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        for (String isbn : parsed.uniqueIsbns()) {
            try {
                ImportOutcome outcome = importByIsbnInternal(isbn, school);
                if (outcome.status() == ImportStatus.ALREADY_EXISTS) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ALREADY_EXISTS,
                            "Boek bestaat al in de bibliotheek.",
                            outcome.bookDto() != null ? outcome.bookDto().getId() : null));
                    continue;
                }

                if (outcome.status() == ImportStatus.NOT_FOUND) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.NOT_FOUND,
                            "Geen boek gevonden voor dit ISBN.",
                            null));
                    continue;
                }

                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ADDED,
                        "Boek toegevoegd.",
                        outcome.bookDto() != null ? outcome.bookDto().getId() : null));
            } catch (Exception ex) {
                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ERROR,
                        "Fout bij verwerken van ISBN.",
                        null));
            }
        }

        result.setResults(rows);
        return result;
    }

    private ImportOutcome importByIsbnInternal(String isbn, School school) {
        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId());
        if (existing.isPresent()) {
            return new ImportOutcome(ImportStatus.ALREADY_EXISTS, BookMapper.toDto(existing.get()));
        }

        Book fetched = openLibraryService.fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return new ImportOutcome(ImportStatus.NOT_FOUND, null);
        }

        fetched.setIsbn(isbnService.normalizeAndValidateIsbn(fetched.getIsbn())
                .orElse(fetched.getIsbn()));

        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return new ImportOutcome(ImportStatus.ADDED, BookMapper.toDto(saved));
    }

    private enum ImportStatus {
        ADDED,
        ALREADY_EXISTS,
        NOT_FOUND
    }

    private record ImportOutcome(ImportStatus status, BookDto bookDto) {
    }
}
