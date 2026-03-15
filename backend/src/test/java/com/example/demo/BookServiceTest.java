package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.BookService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BookRepository bookRepository;

    @InjectMocks
    private BookService bookService;

    @Mock
    private SchoolService schoolService;

    private School makeSchool() {
        School school = new School();
        school.setId(1L);
        school.setNaam("GO! Atheneum Antwerpen");
        return school;
    }

    private Book makeBook() {
        Book b = new Book();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
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
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.of(makeBook()));

        BookDto result = bookService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verify(bookRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("null")
    void importByIsbnShouldFetchFromOpenLibrarySaveAndReturnNewBook() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(bookRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.empty());
        when(bookRepository.save(any(Book.class))).thenReturn(makeBook());

        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Dune");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL2732061A")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Frank Herbert");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
                when(rt.getForEntity(contains("isbn"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
                when(rt.getForEntity(contains("authors"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BookDto result = bookService.importByIsbn("9780553808049", 1L);

            assertNotNull(result);
            assertEquals("Dune", result.getTitel());
            verify(bookRepository).save(any(Book.class));
        }
    }

    @Test
    @SuppressWarnings("null")
    void importByIsbnShouldReturnNullWhenIsbnNotFoundInOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(bookRepository.findByIsbnAndSchool_Id("0000000000000", 1L)).thenReturn(Optional.empty());

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (rt, ctx) -> when(rt.getForEntity(anyString(), eq(Object.class)))
                        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)))) {
            BookDto result = bookService.importByIsbn("0000000000000", 1L);

            assertNull(result);
            verify(bookRepository, never()).save(any());
        }
    }

    // ---- fetchPreviewByIsbn -------------------------------------------------

    @Test
    @SuppressWarnings("null")
    void fetchPreviewByIsbnShouldReturnDtoWithoutPersisting() {
        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Dune");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL2732061A")));
        bookBody.put("languages", List.of(Map.of("key", "/languages/eng")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Frank Herbert");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
                when(rt.getForEntity(contains("isbn"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
                when(rt.getForEntity(contains("authors"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BookDto result = bookService.fetchPreviewByIsbn("9780553808049");

            assertNotNull(result);
            assertEquals("Dune", result.getTitel());
            assertEquals("Frank Herbert", result.getAuteur());
            assertEquals("Engels", result.getTaal());
            verify(bookRepository, never()).save(any());
        }
    }

    @Test
    @SuppressWarnings("null")
    void fetchPreviewByIsbnShouldMapFrenchLanguageToFrench() {
        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Le Petit Prince");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL1000000A")));
        bookBody.put("languages", List.of(Map.of("key", "/languages/fre")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Antoine de Saint-Exupery");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
                when(rt.getForEntity(contains("isbn"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
                when(rt.getForEntity(contains("authors"), eq(Object.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BookDto result = bookService.fetchPreviewByIsbn("9780156012195");

            assertNotNull(result);
            assertEquals("Frans", result.getTaal());
        }
    }

    @Test
    @SuppressWarnings("null")
    void fetchPreviewByIsbnShouldReturnNullWhenNotFoundInOpenLibrary() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (rt, ctx) -> when(rt.getForEntity(anyString(), eq(Object.class)))
                        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)))) {
            BookDto result = bookService.fetchPreviewByIsbn("0000000000000");

            assertNull(result);
        }
    }
}
