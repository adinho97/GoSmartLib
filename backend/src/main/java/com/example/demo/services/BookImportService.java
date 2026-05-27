package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
public class BookImportService {

    private final BookRepository bookRepository;
    private final BookCopyRepository bookCopyRepository;
    private final SchoolService schoolService;
    private final IsbnService isbnService;
    private final BulkImportService bulkImportService;
    private final ImportCoreService importCoreService;

    public BookImportService(BookRepository bookRepository,
            BookCopyRepository bookCopyRepository,
            SchoolService schoolService,
            IsbnService isbnService,
            BulkImportService bulkImportService,
            ImportCoreService importCoreService) {
        this.bookRepository = bookRepository;
        this.bookCopyRepository = bookCopyRepository;
        this.schoolService = schoolService;
        this.isbnService = isbnService;
        this.bulkImportService = bulkImportService;
        this.importCoreService = importCoreService;
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId) {
        String normalizedIsbn = isbnService.requireValidNormalizedIsbn(isbn);
        School school = schoolService.getByIdOrDefault(schoolId);
        ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(normalizedIsbn, school);
        return outcome.bookDto();
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId) {
        School school = schoolService.getByIdOrDefault(schoolId);
        BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file, null);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.isbnQuantityPairs().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        int totalCopiesAdded = 0;
        for (BulkImportService.IsbnQuantityPair pair : parsed.isbnQuantityPairs()) {
            String isbn = pair.isbn();
            int quantity = pair.quantity();
            BookCopy.CopyCondition condition = pair.condition();
            try {
                ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(isbn, school);
                Book bookToAssociateCopies = null;
                Long bookIdForDiagnostic = null;

                if (outcome.status() == ImportCoreService.ImportStatus.ALREADY_EXISTS) {
                    Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                    if (book != null) {
                        bookToAssociateCopies = book;
                        bookIdForDiagnostic = book.getId();
                        for (int i = 0; i < quantity; i++) {
                            BookCopy copy = new BookCopy();
                            copy.setBook(bookToAssociateCopies);
                            copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                            copy.setCondition(condition);
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
                        copy.setCondition(condition);
                        bookCopyRepository.save(copy);
                    }
                    totalCopiesAdded += quantity;
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ADDED,
                            "Boek toegevoegd met " + quantity + " exemplaar(en).",
                            bookIdForDiagnostic));
                } else {
                        rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ERROR,
                            "Boek is geimporteerd, maar er is een fout opgetreden bij het toevoegen van exemplaren.",
                            bookIdForDiagnostic));
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

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId, String defaultCondition) {
        School school = schoolService.getByIdOrDefault(schoolId);
        BulkImportService.ParsedBulkIsbn parsed = bulkImportService.parseAndValidate(file, defaultCondition);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(parsed.totalRows());
        result.setUniqueIsbnsProcessed(parsed.isbnQuantityPairs().size());
        result.setDuplicateRowsSkipped(parsed.duplicateRowsSkipped());

        List<ImportResultDto.RowResult> rows = new ArrayList<>(parsed.invalidRows());
        int totalCopiesAdded = 0;
        for (BulkImportService.IsbnQuantityPair pair : parsed.isbnQuantityPairs()) {
            String isbn = pair.isbn();
            int quantity = pair.quantity();
            BookCopy.CopyCondition condition = pair.condition();
            try {
                ImportCoreService.ImportOutcome outcome = importCoreService.importByNormalizedIsbn(isbn, school);
                Book bookToAssociateCopies = null;
                Long bookIdForDiagnostic = null;

                if (outcome.status() == ImportCoreService.ImportStatus.ALREADY_EXISTS) {
                    Book book = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId()).orElse(null);
                    if (book != null) {
                        bookToAssociateCopies = book;
                        bookIdForDiagnostic = book.getId();
                        for (int i = 0; i < quantity; i++) {
                            BookCopy copy = new BookCopy();
                            copy.setBook(bookToAssociateCopies);
                            copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
                            copy.setCondition(condition);
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
                        copy.setCondition(condition);
                        bookCopyRepository.save(copy);
                    }
                    totalCopiesAdded += quantity;
                    rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ADDED,
                            "Boek toegevoegd met " + quantity + " exemplaar(en).",
                            bookIdForDiagnostic));
                } else {
                        rows.add(new ImportResultDto.RowResult(
                            isbn,
                            ImportResultDto.Status.ERROR,
                            "Boek is geimporteerd, maar er is een fout opgetreden bij het toevoegen van exemplaren.",
                            bookIdForDiagnostic));
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
}
