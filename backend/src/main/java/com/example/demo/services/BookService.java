package com.example.demo.services;

import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.LeeslijstRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.config.HighlightedBookRepository;
import com.example.demo.config.ClassReadingListItemRepository;
import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Loan;
import com.example.demo.entities.Leeslijst;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.School;
import com.example.demo.exception.ApiException;
import com.example.demo.mappers.BookMapper;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
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
    private final LeeslijstRepository leeslijstRepository;
    private final WishlistRepository wishlistRepository;
    private final HighlightedBookRepository highlightedBookRepository;
    private final ClassReadingListItemRepository classReadingListItemRepository;

    @Autowired
    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository, SchoolService schoolService, OpenLibraryService openLibraryService, 
            IsbnService isbnService, BulkImportService bulkImportService, ImportCoreService importCoreService, 
            LeeslijstRepository leeslijstRepository, WishlistRepository wishlistRepository, 
            HighlightedBookRepository highlightedBookRepository,
            ClassReadingListItemRepository classReadingListItemRepository) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.loanRepository = loanRepository;
        this.schoolService = schoolService;
        this.openLibraryService = openLibraryService;
        this.isbnService = isbnService;
        this.bulkImportService = bulkImportService;
        this.importCoreService = importCoreService;
        this.leeslijstRepository = leeslijstRepository;
        this.wishlistRepository = wishlistRepository;
        this.highlightedBookRepository = highlightedBookRepository;
        this.classReadingListItemRepository = classReadingListItemRepository;
    }

    /**
     * Overloaded constructor for backwards compatibility with existing tests.
     */
    public BookService(BookRepository bookRepository, BookCopyRepository bookCopyRepository,
            LoanRepository loanRepository, SchoolService schoolService, OpenLibraryService openLibraryService,
            IsbnService isbnService, BulkImportService bulkImportService,
            ImportCoreService importCoreService) {
        this(bookRepository, bookCopyRepository, loanRepository, schoolService, openLibraryService,
                isbnService, bulkImportService, importCoreService, null, null, null, null);
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
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        if (schoolId != null && (book.getSchool() == null || !book.getSchool().getId().equals(schoolId))) {
            throw new ApiException("Dit boek behoort niet tot jouw school.", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!loanRepository.findByCopy_Book_IdAndReturnedAtIsNull(id).isEmpty()) {
            throw new ApiException("Kan boek niet verwijderen: er zijn nog actieve uitleningen.", HttpStatus.CONFLICT, "ACTIVE_LOANS_EXIST");
        }

        // 1. Clear Loan history for this book's copies
        List<Loan> bookLoans = loanRepository.findAll().stream()
                .filter(l -> l.getCopy().getBook().getId().equals(id))
                .toList();
        loanRepository.deleteAll(bookLoans);

        // 2. Clear Wishlists
        if (wishlistRepository != null) {
            var wishes = wishlistRepository.findAll().stream()
                    .filter(w -> w.getBook().getId().equals(id))
                    .toList();
            wishlistRepository.deleteAll(wishes);
        }

        // 3. Clear Highlighted status
        if (highlightedBookRepository != null) {
            var highlights = highlightedBookRepository.findAll().stream()
                    .filter(h -> id.equals(h.getBookId()))
                    .toList();
            highlightedBookRepository.deleteAll(highlights);
        }

        // 4. Clear Class Reading List items
        if (classReadingListItemRepository != null) {
            var classItems = classReadingListItemRepository.findAll().stream()
                    .filter(c -> id.equals(c.getBookId()))
                    .toList();
            classReadingListItemRepository.deleteAll(classItems);
        }

        // 5. Remove book from all reading lists (Leeslijsten) to clear join table
        if (leeslijstRepository != null) {
            leeslijstRepository.findAll().forEach(list -> {
                Set<Book> booksInList = new HashSet<>(list.getBooks()); // Create a mutable copy
                boolean changed = booksInList.removeIf(b -> b.getId().equals(id));
                if (changed) {
                    list.setBooks(booksInList); // Set the new collection
                    leeslijstRepository.save(list);
                }
            });
            leeslijstRepository.flush();
        }

        // 6. Delete all copies associated with this book.
        List<BookCopy> bookCopies = bookCopyRepository.findAll().stream()
                .filter(c -> c.getBook().getId().equals(id))
                .toList();
        bookCopyRepository.deleteAll(bookCopies);

        bookRepository.delete(book);
    }

    @Transactional
    public Leeslijst createLeeslijst(String titel, String description, List<Long> bookIds, List<Long> klasIds, boolean isGlobal, List<String> sharedWithUserSubs) {
        // Placeholder for actual Leeslijst creation logic
        // In a real scenario, you would create a Leeslijst entity, set its properties
        // including isGlobal and sharedWithUserSubs, and save it.
        // For now, we just return a dummy Leeslijst.
        Leeslijst dummyLeeslijst = new Leeslijst();
        dummyLeeslijst.setTitel(titel);
        dummyLeeslijst.setDescription(description);
        // Assume other fields are set
        // dummyLeeslijst.setGlobal(isGlobal);
        // dummyLeeslijst.setSharedWithUserSubs(sharedWithUserSubs);
        return dummyLeeslijst;
    }

    @Transactional
    public Leeslijst updateLeeslijst(Long id, String titel, String description, List<Long> bookIds, List<Long> klasIds, boolean isGlobal, List<String> sharedWithUserSubs) {
        // Placeholder for actual Leeslijst update logic
        // In a real scenario, you would fetch the existing Leeslijst, update its properties
        // including isGlobal and sharedWithUserSubs, and save it.
        // For now, we just return a dummy Leeslijst.
        Leeslijst dummyLeeslijst = new Leeslijst();
        dummyLeeslijst.setId(id);
        dummyLeeslijst.setTitel(titel);
        dummyLeeslijst.setDescription(description);
        // Assume other fields are set
        // dummyLeeslijst.setGlobal(isGlobal);
        // dummyLeeslijst.setSharedWithUserSubs(sharedWithUserSubs);
        return dummyLeeslijst;
    }
}
