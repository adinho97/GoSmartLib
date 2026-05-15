package com.example.demo.services;

import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final LoanRepository loanRepository;
    private final SchoolService schoolService;
    private final OpenLibraryService openLibraryService;
    private final IsbnService isbnService;
    private final BulkImportService bulkImportService;
    private final ImportCoreService importCoreService;

    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository, SchoolService schoolService, OpenLibraryService openLibraryService,
            IsbnService isbnService, BulkImportService bulkImportService,
            ImportCoreService importCoreService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
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
                Book bookToAssociateCopies = null;
                Long bookIdForDiagnostic = null;

                if (outcome.status() == ImportCoreService.ImportStatus.ALREADY_EXISTS) {
                    // Book already exists, so just add the copies
                    Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                    if (book != null) {
                        bookToAssociateCopies = book;
                        bookIdForDiagnostic = book.getId();
                        for (int i = 0; i < quantity; i++) {
                            BookCopy copy = new BookCopy();
                            copy.setBook(bookToAssociateCopies);
                            copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                            bookCopyRepository.save(copy);
                        }
                        totalCopiesAdded += quantity;
                        rows.add(new ImportResultDto.RowResult(
                                isbn,
                                ImportResultDto.Status.ADDED,
                                "Boek al in bibliotheek - " + quantity + " exemplaren toegevoegd.",
                                bookIdForDiagnostic));
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

                // If we reach here, it means the book was NEWLY_ADDED by importCoreService.importByNormalizedIsbn
                // We should use the book entity from the outcome directly if possible,
                // or fetch it using the ID from the outcome's BookDto.
                BookDto newlyAddedBookDto = outcome.bookDto();
                if (newlyAddedBookDto != null && newlyAddedBookDto.getId() != null) {
                    bookToAssociateCopies = bookRepository.findById(newlyAddedBookDto.getId()).orElse(null);
                    bookIdForDiagnostic = newlyAddedBookDto.getId();
                }

                if (bookToAssociateCopies != null) {
                    for (int i = 0; i < quantity; i++) {
                        BookCopy copy = new BookCopy();
                        copy.setBook(bookToAssociateCopies);
                        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                        bookCopyRepository.save(copy);
                    }
                    totalCopiesAdded += quantity;
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ADDED,
                            "Boek toegevoegd met " + quantity + " exemplaar(en).",
                            bookIdForDiagnostic));
                } else {
                    // This scenario means importCoreService reported success (not NOT_FOUND or ALREADY_EXISTS)
                    // but we couldn't get the Book entity to add copies. This is an unexpected error.
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ERROR,
                            "Boek is geïmporteerd, maar er is een fout opgetreden bij het toevoegen van exemplaren.",
                            bookIdForDiagnostic)); // Use bookIdForDiagnostic if available, even if bookToAssociateCopies is null
                }
            } catch (Exception ex) {
                rows.add(new ImportResultDto.RowResult(
                        isbn,
                        ImportResultDto.Status.ERROR,
                        "Fout bij verwerken van ISBN: " + ex.getMessage(),
                        null));
            }
        }

        result.setTotalCopiesAdded(totalCopiesAdded);
        result.setResults(rows);
        return result;
    }

    /**
     * Get all books with loan statistics, sorted by loan count descending.
     * This is used for the popularity catalog feature.
     */
    public List<BookDto> getBooksWithStats() {
        List<Book> books = bookRepository.findAll();
        Map<Long, Long> loanCountMap = buildLoanCountMap();

        return books.stream()
                .map(BookMapper::toDto)
                .peek(dto -> dto.setLoanCount(loanCountMap.getOrDefault(dto.getId(), 0L)))
                .sorted((a, b) -> Long.compare(b.getLoanCount(), a.getLoanCount()))
                .collect(Collectors.toList());
    }

    /**
     * Build a map of book ID to total loan count (all loans, including returned).
     */
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

    /**
     * Deletes a book from the library catalog after verifying school ownership
     * and ensuring no active loans exist for the book.
     */
    @Transactional
    public void deleteBook(Long id, Long schoolId) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Boek niet gevonden"));

        if (schoolId != null && (book.getSchool() == null || !book.getSchool().getId().equals(schoolId))) {
            throw new IllegalArgumentException("Dit boek behoort niet tot jouw school.");
        }

        if (!loanRepository.findByCopy_Book_IdAndReturnedAtIsNull(id).isEmpty()) {
            throw new IllegalStateException("Kan boek niet verwijderen: er zijn nog actieve uitleningen.");
        }

        bookRepository.delete(book);
    }
}
