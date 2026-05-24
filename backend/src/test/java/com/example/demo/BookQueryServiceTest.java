package com.example.demo;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.PagedBookResponse;
import com.example.demo.entities.Book;
import com.example.demo.entities.Genre;
import com.example.demo.exception.ApiException;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import com.example.demo.services.BookQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BookQueryServiceTest {

    @Mock
    private BookRepository bookRepository;

    @Mock
    private BookMapper bookMapper;

    @InjectMocks
    private BookQueryService bookQueryService;

    private Book makeBook(Long id, String title, String... genreNames) {
        Book b = new Book();
        b.setId(id);
        b.setTitel(title);
        Set<Genre> genres = new HashSet<>();
        for (String name : genreNames) {
            Genre g = new Genre();
            g.setNaam(name);
            genres.add(g);
        }
        b.setGenres(genres);
        return b;
    }

    private BookDto dto(Long id, String title) {
        BookDto d = new BookDto();
        d.setId(id);
        d.setTitel(title);
        return d;
    }

    @Test
    void listAllShouldUseNonDidacticQueryForStudentsWithSchool() {
        Book b = makeBook(1L, "X");
        when(bookRepository.findNonDidacticBySchool_Id(7L)).thenReturn(List.of(b));
        when(bookMapper.toDto(b)).thenReturn(dto(1L, "X"));

        List<BookDto> result = bookQueryService.listAll(7L, true);

        assertEquals(1, result.size());
        verify(bookRepository).findNonDidacticBySchool_Id(7L);
        verify(bookRepository, never()).findAllBySchool_Id(any());
    }

    @Test
    void listAllShouldUseSchoolScopedQueryForNonStudent() {
        Book b = makeBook(1L, "X");
        when(bookRepository.findAllBySchool_Id(7L)).thenReturn(List.of(b));
        when(bookMapper.toDto(b)).thenReturn(dto(1L, "X"));

        bookQueryService.listAll(7L, false);

        verify(bookRepository).findAllBySchool_Id(7L);
        verify(bookRepository, never()).findNonDidacticBySchool_Id(any());
        verify(bookRepository, never()).findAll();
    }

    @Test
    void listAllShouldUseUnscopedQueryWhenNoSchool() {
        when(bookRepository.findAll()).thenReturn(List.of());

        bookQueryService.listAll(null, false);

        verify(bookRepository).findAll();
    }

    @Test
    void listDidacticCollectionShouldFilterOnDidactiekGenre() {
        Book didactic = makeBook(1L, "DidacticBook", "didactiek-iets");
        Book regular = makeBook(2L, "Roman", "Roman");
        when(bookRepository.findAllBySchool_Id(7L)).thenReturn(List.of(didactic, regular));
        when(bookMapper.toDto(didactic)).thenReturn(dto(1L, "DidacticBook"));

        List<BookDto> result = bookQueryService.listDidacticCollection(7L);

        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getId());
    }

    @Test
    void getBookShouldThrowNotFoundWhenMissing() {
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> bookQueryService.getBook(99L, null, false));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getBookShouldThrowNotFoundForStudentRequestingDidacticBook() {
        Book didactic = makeBook(1L, "X", "Didactiek");
        when(bookRepository.findByIdAndSchool_Id(1L, 7L)).thenReturn(Optional.of(didactic));

        ApiException ex = assertThrows(ApiException.class,
                () -> bookQueryService.getBook(1L, 7L, true));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void getBookShouldReturnDtoForNonDidacticBook() {
        Book book = makeBook(1L, "Roman", "Roman");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(bookMapper.toDto(book)).thenReturn(dto(1L, "Roman"));

        BookDto result = bookQueryService.getBook(1L, null, false);

        assertEquals(1L, result.getId());
    }

    @Test
    void getBookByGoNumberShouldThrowBadRequestForBlankInput() {
        ApiException ex = assertThrows(ApiException.class,
                () -> bookQueryService.getBookByGoNumber("   ", null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    void getBookByGoNumberShouldUppercaseAndQuery() {
        Book b = makeBook(1L, "X");
        when(bookRepository.findByGoNumber("GO-00000001")).thenReturn(Optional.of(b));
        when(bookMapper.toDto(b)).thenReturn(dto(1L, "X"));

        BookDto result = bookQueryService.getBookByGoNumber("  go-00000001  ", null);

        assertEquals(1L, result.getId());
    }

    @Test
    void getBookByGoNumberShouldThrowNotFoundWhenMissing() {
        when(bookRepository.findByGoNumberAndSchool_Id("GO-00000001", 5L))
                .thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> bookQueryService.getBookByGoNumber("GO-00000001", 5L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void searchPagedShouldClampPageAndSize() {
        Page<Book> empty = new PageImpl<>(List.of());
        when(bookRepository.searchPaged(eq(null), eq("dune"), eq(false), any(PageRequest.class)))
                .thenReturn(empty);

        PagedBookResponse result = bookQueryService.searchPaged(null, "  dune  ", false, -3, 0);

        assertEquals(0, result.getTotal());
        verify(bookRepository).searchPaged(eq(null), eq("dune"), eq(false), any(PageRequest.class));
    }
}
