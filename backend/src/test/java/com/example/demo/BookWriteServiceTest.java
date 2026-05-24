package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.exception.ApiException;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.BookWriteService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookWriteServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @Mock
    private SchoolService schoolService;

    @InjectMocks
    private BookWriteService bookWriteService;

    private School school;

    @BeforeEach
    void setUp() {
        school = new School();
        school.setId(7L);
    }

    private BookDto dto(String title, String isbn) {
        BookDto d = new BookDto();
        d.setTitel(title);
        d.setIsbn(isbn);
        d.setSchoolId(7L);
        return d;
    }

    @Test
    void createShouldThrowBadRequestWhenSchoolInvalid() {
        when(schoolService.getByIdOrDefault(7L)).thenThrow(new IllegalArgumentException("bad school"));

        ApiException ex = assertThrows(ApiException.class,
                () -> bookWriteService.create(dto("X", "9781234567890")));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void createShouldThrowConflictWhenIsbnAlreadyExistsForSchool() {
        when(schoolService.getByIdOrDefault(7L)).thenReturn(school);
        when(bookRepository.findByIsbnAndSchool_Id("9781234567890", 7L))
                .thenReturn(Optional.of(new Book()));

        ApiException ex = assertThrows(ApiException.class,
                () -> bookWriteService.create(dto("X", "9781234567890")));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        verify(bookRepository, never()).save(any());
    }

    @Test
    void createShouldGenerateGoNumberWhenIsbnIsMissing() {
        when(schoolService.getByIdOrDefault(7L)).thenReturn(school);
        Book mapped = new Book();
        when(bookMapper.toEntity(any(BookDto.class))).thenReturn(mapped);
        when(bookRepository.existsByGoNumber(any())).thenReturn(false);
        Book saved = new Book();
        saved.setId(42L);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);
        when(bookRepository.findById(42L)).thenReturn(Optional.of(saved));
        BookDto savedDto = new BookDto();
        savedDto.setId(42L);
        when(bookMapper.toDto(saved)).thenReturn(savedDto);

        bookWriteService.create(dto("X", null));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertNotNull(captor.getValue().getGoNumber(), "GO number should be assigned when ISBN missing");
        assertTrue(captor.getValue().getGoNumber().startsWith("GO-"));
    }

    @Test
    void createShouldNotGenerateGoNumberWhenIsbnIsProvided() {
        when(schoolService.getByIdOrDefault(7L)).thenReturn(school);
        when(bookRepository.findByIsbnAndSchool_Id(any(), any())).thenReturn(Optional.empty());
        Book mapped = new Book();
        mapped.setIsbn("9781234567890");
        when(bookMapper.toEntity(any(BookDto.class))).thenReturn(mapped);
        Book saved = new Book();
        saved.setId(42L);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);
        when(bookRepository.findById(42L)).thenReturn(Optional.of(saved));
        when(bookMapper.toDto(saved)).thenReturn(new BookDto());

        bookWriteService.create(dto("X", "9781234567890"));

        ArgumentCaptor<Book> captor = ArgumentCaptor.forClass(Book.class);
        verify(bookRepository).save(captor.capture());
        assertNull(captor.getValue().getGoNumber(), "GO number should NOT be assigned when ISBN is present");
    }

    @Test
    void createShouldRetryGoNumberWhenCandidateExists() {
        when(schoolService.getByIdOrDefault(7L)).thenReturn(school);
        Book mapped = new Book();
        when(bookMapper.toEntity(any(BookDto.class))).thenReturn(mapped);
        when(bookRepository.existsByGoNumber(any())).thenReturn(true, true, false);
        Book saved = new Book();
        saved.setId(42L);
        when(bookRepository.save(any(Book.class))).thenReturn(saved);
        when(bookRepository.findById(42L)).thenReturn(Optional.of(saved));
        when(bookMapper.toDto(saved)).thenReturn(new BookDto());

        bookWriteService.create(dto("X", null));

        verify(bookRepository, times(3)).existsByGoNumber(any());
    }

    @Test
    void updateShouldThrowNotFoundWhenBookMissing() {
        BookDto patch = new BookDto();
        patch.setTitel("X");
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> bookWriteService.update(99L, patch));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void updateShouldApplyFieldsAndSave() {
        Book existing = new Book();
        existing.setId(1L);
        existing.setTitel("OldTitle");

        BookDto patch = new BookDto();
        patch.setTitel("NewTitle");
        patch.setAuteur("NewAuthor");
        patch.setIsbn("9781234567890");

        Book mappedForGenres = new Book();
        when(bookMapper.toEntity(patch)).thenReturn(mappedForGenres);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(existing));
        BookDto result = new BookDto();
        result.setId(1L);
        result.setTitel("NewTitle");
        when(bookMapper.toDto(existing)).thenReturn(result);

        BookDto out = bookWriteService.update(1L, patch);

        assertEquals("NewTitle", out.getTitel());
        assertEquals("NewTitle", existing.getTitel());
        assertEquals("NewAuthor", existing.getAuteur());
        verify(bookRepository).save(existing);
    }
}
