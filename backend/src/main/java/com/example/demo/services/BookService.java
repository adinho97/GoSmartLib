package com.example.demo.services;

import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
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
    private final BookCopyRepository bookCopyRepository;
    private final SchoolService schoolService;
    private final OpenLibraryService openLibraryService;
    private final IsbnService isbnService;
    private final BulkImportService bulkImportService;
    private final ImportCoreService importCoreService;

    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
            SchoolService schoolService, OpenLibraryService openLibraryService,
            IsbnService isbnService, BulkImportService bulkImportService,
            ImportCoreService importCoreService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.schoolService = schoolService;
        this.openLibraryService = openLibraryService;
        this.isbnService = isbnService;
        this.bulkImportService = bulkImportService;
        this.importCoreService = importCoreService;
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
        ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(normalizedIsbn, school);
        return outcome.bookDto();
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId) {
        // Resolve school once; bulk uses the same school-scoped behavior as single ISBN
        // import.
        School school = schoolService.getByIdOrDefault(schoolId);
        BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.isbnQuantityPairs().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        int totalCopiesAdded = 0;
        for (BulkImportService.IsbnQuantityPair pair : parsed.isbnQuantityPairs()) {
            String isbn = pair.isbn();
            int quantity = pair.quantity();
            try {
                ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(isbn, school);
                if (outcome.status() == ImportCoreService.ImportStatus.ALREADY_EXISTS) {
                    // Book already exists, so just add the copies
                    Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                    if (book != null) {
                        for (int i = 0; i < quantity; i++) {
                            BookCopy copy = new BookCopy();
                            copy.setBook(book);
                            copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                            bookCopyRepository.save(copy);
                        }
                        totalCopiesAdded += quantity;
                        rows.add(new ImportResultDto.RowResult(
                                isbn,
                                ImportResultDto.Status.ADDED,
                                "Boek al in bibliotheek - " + quantity + " exemplaren toegevoegd.",
                                outcome.bookDto() != null ? outcome.bookDto().getId() : null));
                    } else {
                        rows.add(new ImportResultDto.RowResult(
                                isbn,
                                ImportResultDto.Status.ERROR,
                                "Boek bestaat maar kon niet worden gevonden.",
                                null));
                    }
                    continue;
                }

                if (outcome.status() == ImportCoreService.ImportStatus.NOT_FOUND) {
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.NOT_FOUND,
                            "Geen boek gevonden voor dit ISBN.",
                            null));
                    continue;
                }

                // Book was successfully added, now create the specified number of copies
                Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                if (book != null) {
                    for (int i = 0; i < quantity; i++) {
                        BookCopy copy = new BookCopy();
                        copy.setBook(book);
                        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                        bookCopyRepository.save(copy);
                    }
                    totalCopiesAdded += quantity;
                }

                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ADDED,
                        "Boek toegevoegd met " + quantity + " exemplaar(en).",
                        outcome.bookDto() != null ? outcome.bookDto().getId() : null));
            } catch (Exception ex) {
                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ERROR,
                        "Fout bij verwerken van ISBN.",
                        null));
            }
        }

        result.setTotalCopiesAdded(totalCopiesAdded);
        result.setResults(rows);
        return result;
    }
}
