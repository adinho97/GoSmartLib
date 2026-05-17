package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.services.BulkImportService;
import com.example.demo.services.BookService;
import com.example.demo.services.ImportCoreService;
import com.example.demo.services.IsbnService;
import com.example.demo.services.OpenLibraryService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.lang.NonNull;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookCopyRepository bookCopyRepository;

    @Mock
    private OpenLibraryService openLibraryService;

    @Spy
    private IsbnService isbnService;

    @Mock
    private BulkImportService bulkImportService;

    @Mock
    private ImportCoreService importCoreService;

    @InjectMocks
    private BookService bookService;

    @Mock
    private SchoolService schoolService;

    private @NonNull School makeSchool() {
        School school = new School();
        school.setId(1L);
        school.setNaam("GO! Atheneum Antwerpen");
        return school;
    }

    private @NonNull Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

    private @NonNull BookDto makeBookDto() {
        BookDto dto = new BookDto();
        dto.setId(1L);
        dto.setTitel("Dune");
        dto.setAuteur("Frank Herbert");
        dto.setIsbn("9780553808049");
        return dto;
    }

    // ---- findByIsbn ---------------------------------------------------------

    @Test
    void findByIsbnShouldReturnDtoWhenBookIsInDb() {
        when(bookRepository.findByIsbn("9780553808049")).thenReturn(Optional.of(makeBook()));

        Optional<BookDto> result = bookService.findByIsbn("9780553808049", null);

        assertTrue(result.isPresent());
        assertEquals("Dune", result.get().getTitel());
        assertEquals("Frank Herbert", result.get().getAuteur());
        assertEquals("9780553808049", result.get().getIsbn());
    }

    @Test
    void findByIsbnShouldReturnEmptyWhenNotInDb() {
        when(bookRepository.findByIsbn("0000000000000")).thenReturn(Optional.empty());

        Optional<BookDto> result = bookService.findByIsbn("0000000000000", null);

        assertFalse(result.isPresent());
    }

    // ---- importByIsbn -------------------------------------------------------

    @Test
    void importByIsbnShouldReturnExistingBookWithoutCallingOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(importCoreService.importByNormalizedIsbn(eq("9780553808049"), any(School.class)))
                .thenReturn(new ImportCoreService.ImportOutcome(ImportCoreService.ImportStatus.ALREADY_EXISTS,
                        makeBookDto()));

        BookDto result = bookService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verifyNoInteractions(bookRepository);
        verify(importCoreService).importByNormalizedIsbn(eq("9780553808049"), any(School.class));
    }

    @Test
    void importByIsbnShouldFetchFromOpenLibrarySaveAndReturnNewBook() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(importCoreService.importByNormalizedIsbn(eq("9780553808049"), any(School.class)))
                .thenReturn(new ImportCoreService.ImportOutcome(ImportCoreService.ImportStatus.ADDED,
                        makeBookDto()));

        BookDto result = bookService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verify(importCoreService).importByNormalizedIsbn(eq("9780553808049"), any(School.class));
    }

    @Test
    void importByIsbnShouldReturnNullWhenIsbnNotFoundInOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(importCoreService.importByNormalizedIsbn(eq("0000000000000"), any(School.class)))
                .thenReturn(new ImportCoreService.ImportOutcome(ImportCoreService.ImportStatus.NOT_FOUND, null));

        BookDto result = bookService.importByIsbn("0000000000000", 1L);

        assertNull(result);
        verifyNoInteractions(bookRepository);
        verify(importCoreService).importByNormalizedIsbn(eq("0000000000000"), any(School.class));
    }

    // ---- fetchPreviewByIsbn -------------------------------------------------

    @Test
    void fetchPreviewByIsbnShouldReturnDtoWithoutPersisting() {
        Book preview = makeBook();
        preview.setTaal("Engels");
        when(openLibraryService.fetchBookFromOpenLibrary("9780553808049")).thenReturn(preview);

        BookDto result = bookService.fetchPreviewByIsbn("9780553808049");

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        assertEquals("Frank Herbert", result.getAuteur());
        assertEquals("Engels", result.getTaal());
        verifyNoInteractions(bookRepository);
        verify(openLibraryService).fetchBookFromOpenLibrary("9780553808049");
    }

    @Test
    void fetchPreviewByIsbnShouldMapFrenchLanguageToFrench() {
        Book preview = makeBook();
        preview.setTaal("Frans");
        when(openLibraryService.fetchBookFromOpenLibrary("9780156012195")).thenReturn(preview);

        BookDto result = bookService.fetchPreviewByIsbn("9780156012195");

        assertNotNull(result);
        assertEquals("Frans", result.getTaal());
        verify(openLibraryService).fetchBookFromOpenLibrary("9780156012195");
    }

    @Test
    void fetchPreviewByIsbnShouldReturnNullWhenNotFoundInOpenLibrary() {
        when(openLibraryService.fetchBookFromOpenLibrary("0000000000000")).thenReturn(null);

        BookDto result = bookService.fetchPreviewByIsbn("0000000000000");

        assertNull(result);
        verify(openLibraryService).fetchBookFromOpenLibrary("0000000000000");
    }

    // ---- importBulkByIsbn ---------------------------------------------------

    @Test
    void importBulkByIsbnShouldAggregateInvalidAndCoreOutcomes() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();
                Book existingBook = makeBook();
                existingBook.setIsbn("9780156012195");
                existingBook.setId(2L);

        List<ImportResultDto.RowResult> invalidRows = List.of(
                new ImportResultDto.RowResult("bad-isbn", ImportResultDto.Status.INVALID_ISBN,
                        "Ongeldig ISBN-formaat", null));

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(
                        new BulkImportService.IsbnQuantityPair("9780553808049", 1),
                        new BulkImportService.IsbnQuantityPair("9780156012195", 2),
                        new BulkImportService.IsbnQuantityPair("0000000000000", 1)),
                invalidRows,
                4,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenReturn(new ImportCoreService.ImportOutcome(ImportCoreService.ImportStatus.ADDED, makeBookDto()));
        when(importCoreService.importByNormalizedIsbn("9780156012195", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ALREADY_EXISTS,
                        makeBookDto()));
        when(importCoreService.importByNormalizedIsbn("0000000000000", school))
                .thenReturn(new ImportCoreService.ImportOutcome(ImportCoreService.ImportStatus.NOT_FOUND, null));
        // ADDED branch resolves the new book via findById(dto.getId());
        // ALREADY_EXISTS branch resolves it via findByIsbnAndSchool_Id.
        when(bookRepository.findById(1L)).thenReturn(Optional.of(makeBook()));
        when(bookRepository.findByIsbnAndSchool_Id("9780156012195", 1L))
                .thenReturn(Optional.of(existingBook));

        ImportResultDto result = bookService.importBulkByIsbn(file, 1L);

        assertEquals(4, result.getTotalRows());
        assertEquals(3, result.getUniqueIsbnsProcessed());
        assertEquals(0, result.getDuplicateRowsSkipped());
        assertEquals(4, result.getResults().size());

        List<ImportResultDto.Status> statuses = new ArrayList<>();
        for (ImportResultDto.RowResult row : result.getResults()) {
            statuses.add(row.status());
        }
        assertEquals(
                List.of(
                        ImportResultDto.Status.INVALID_ISBN,
                        ImportResultDto.Status.ADDED,
                        ImportResultDto.Status.ADDED,
                        ImportResultDto.Status.NOT_FOUND),
                statuses);
    }

    @Test
    void importBulkByIsbnShouldMarkErrorWhenCoreThrows() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(new BulkImportService.IsbnQuantityPair("9780553808049", 1)),
                List.of(),
                1,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenThrow(new RuntimeException("boom"));

        ImportResultDto result = bookService.importBulkByIsbn(file, 1L);

        assertEquals(1, result.getResults().size());
        ImportResultDto.RowResult row = result.getResults().get(0);
        assertEquals(ImportResultDto.Status.ERROR, row.status());
        assertEquals("Fout bij verwerken van ISBN: boom", row.message());
    }

    // ---- Copy Creation Tests ------------------------------------------------

    @Test
    void importBulkByIsbnShouldCreateMultipleCopiesForNewBooks() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();
        Book savedBook = makeBook();

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(new BulkImportService.IsbnQuantityPair("9780553808049", 3)),
                List.of(),
                1,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ADDED, makeBookDto()));
        // ADDED branch resolves the new book via findById(dto.getId()).
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedBook));

        ImportResultDto result = bookService.importBulkByIsbn(file, 1L);

        // Verify 3 copies were created
        ArgumentCaptor<BookCopy> newBookCopiesCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository, times(3)).save(newBookCopiesCaptor.capture());
        
        // Verify totalCopiesAdded is 3
        assertEquals(3, result.getTotalCopiesAdded());
        
        // Verify message contains quantity
        ImportResultDto.RowResult row = result.getResults().get(0);
        assertEquals(ImportResultDto.Status.ADDED, row.status());
        assertTrue(row.message().contains("3 exemplaar(en)"));
    }

    @Test
    void importBulkByIsbnShouldCreateMultipleCopiesForExistingBooks() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();
        Book existingBook = makeBook();

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(new BulkImportService.IsbnQuantityPair("9780553808049", 5)),
                List.of(),
                1,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ALREADY_EXISTS, makeBookDto()));
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L))
                .thenReturn(Optional.of(existingBook));

        ImportResultDto result = bookService.importBulkByIsbn(file, 1L);

        // Verify 5 copies were created
        ArgumentCaptor<BookCopy> existingBookCopiesCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository, times(5)).save(existingBookCopiesCaptor.capture());
        
        // Verify totalCopiesAdded is 5
        assertEquals(5, result.getTotalCopiesAdded());
        
        // Verify message for existing book
        ImportResultDto.RowResult row = result.getResults().get(0);
        assertEquals(ImportResultDto.Status.ADDED, row.status());
        assertTrue(row.message().contains("Boek al in bibliotheek"));
        assertTrue(row.message().contains("5 exemplaren"));
    }

    @Test
    void importBulkByIsbnShouldAccumulateTotalCopiesAcrossMultipleBooks() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();
        Book book1 = makeBook();
        Book book2 = makeBook();
        book2.setIsbn("9780156012195");
        book2.setId(2L);

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(
                        new BulkImportService.IsbnQuantityPair("9780553808049", 2),
                        new BulkImportService.IsbnQuantityPair("9780156012195", 3)),
                List.of(),
                2,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ADDED, makeBookDto()));
        when(importCoreService.importByNormalizedIsbn("9780156012195", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ALREADY_EXISTS, makeBookDto()));
        // ADDED branch resolves the book via findById(dto.getId());
        // ALREADY_EXISTS branch resolves it via findByIsbnAndSchool_Id.
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book1));
        when(bookRepository.findByIsbnAndSchool_Id("9780156012195", 1L))
                .thenReturn(Optional.of(book2));

        ImportResultDto result = bookService.importBulkByIsbn(file, 1L);

        // Verify total is 2 + 3 = 5 copies
        assertEquals(5, result.getTotalCopiesAdded());
        
        // Verify copies were saved (2 + 3 = 5 times)
        ArgumentCaptor<BookCopy> totalCopiesCaptor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository, times(5)).save(totalCopiesCaptor.capture());
    }

    @Test
    void importBulkByIsbnShouldSetAllCopiesStatusToAvailable() {
        MultipartFile file = mock(MultipartFile.class);
        School school = makeSchool();
        Book savedBook = makeBook();

        BulkImportService.ParsedBulkIsbn parsed = new BulkImportService.ParsedBulkIsbn(
                List.of(new BulkImportService.IsbnQuantityPair("9780553808049", 2)),
                List.of(),
                1,
                0);

        when(schoolService.getByIdOrDefault(1L)).thenReturn(school);
        when(bulkImportService.parseAndValidate(file)).thenReturn(parsed);
        when(importCoreService.importByNormalizedIsbn("9780553808049", school))
                .thenReturn(new ImportCoreService.ImportOutcome(
                        ImportCoreService.ImportStatus.ADDED, makeBookDto()));
        // ADDED branch resolves the new book via findById(dto.getId()).
        when(bookRepository.findById(1L)).thenReturn(Optional.of(savedBook));

        bookService.importBulkByIsbn(file, 1L);

        // Verify each saved copy has AVAILABLE status
        ArgumentCaptor<BookCopy> captor = ArgumentCaptor.forClass(BookCopy.class);
        verify(bookCopyRepository, times(2)).save(captor.capture());
        
        for (BookCopy copy : captor.getAllValues()) {
            assertEquals(BookCopy.CopyStatus.AVAILABLE, copy.getStatus());
            assertEquals(savedBook.getId(), copy.getBook().getId());
        }
    }
}
