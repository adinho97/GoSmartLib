package com.example.demo.services;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.PagedBookResponse;
import com.example.demo.entities.Book;
import com.example.demo.exception.ApiException;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class BookQueryService {

    private final BookRepository bookRepository;
    private final BookMapper bookMapper;

    public BookQueryService(BookRepository bookRepository, BookMapper bookMapper) {
        this.bookRepository = bookRepository;
        this.bookMapper = bookMapper;
    }

    public List<BookDto> listAll(Long effectiveSchoolId, boolean excludeDidactic) {
        List<Book> books;
        if (excludeDidactic && effectiveSchoolId != null) {
            books = bookRepository.findNonDidacticBySchool_Id(effectiveSchoolId);
        } else {
            books = effectiveSchoolId == null
                    ? bookRepository.findAll()
                    : bookRepository.findAllBySchool_Id(effectiveSchoolId);
        }
        return books.stream().map(bookMapper::toDto).collect(Collectors.toList());
    }

    public List<BookDto> listDidacticCollection(Long effectiveSchoolId) {
        List<Book> books = effectiveSchoolId == null
                ? bookRepository.findAll()
                : bookRepository.findAllBySchool_Id(effectiveSchoolId);
        return books.stream()
                .filter(book -> book.getGenres() != null && !book.getGenres().isEmpty())
                .filter(BookQueryService::hasDidactiekGenre)
                .map(bookMapper::toDto)
                .collect(Collectors.toList());
    }

    public PagedBookResponse searchPaged(Long effectiveSchoolId, String query,
            boolean excludeDidactic, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(size, 1);
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;

        Page<Book> books = bookRepository.searchPaged(
                effectiveSchoolId, normalizedQuery, excludeDidactic,
                PageRequest.of(safePage, safeSize));

        List<BookDto> items = books.stream().map(bookMapper::toDto).collect(Collectors.toList());
        return new PagedBookResponse(items, books.getTotalElements());
    }

    public BookDto getBook(Long bookId, Long effectiveSchoolId, boolean excludeDidactic) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        var bookOpt = effectiveSchoolId == null
                ? bookRepository.findById(resolvedBookId)
                : bookRepository.findByIdAndSchool_Id(resolvedBookId, effectiveSchoolId);
        Book book = bookOpt.orElseThrow(
                () -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        if (excludeDidactic && hasDidactiekGenre(book)) {
            throw new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND");
        }
        return bookMapper.toDto(book);
    }

    public BookDto getBookByGoNumber(String goNumber, Long effectiveSchoolId) {
        if (!StringUtils.hasText(goNumber)) {
            throw new ApiException("GO-nummer is verplicht", HttpStatus.BAD_REQUEST, "INVALID_GO_NUMBER");
        }
        String trimmedGoNumber = goNumber.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(trimmedGoNumber)) {
            throw new ApiException("GO-nummer is verplicht", HttpStatus.BAD_REQUEST, "INVALID_GO_NUMBER");
        }
        return (effectiveSchoolId == null
                ? bookRepository.findByGoNumber(trimmedGoNumber)
                : bookRepository.findByGoNumberAndSchool_Id(trimmedGoNumber, effectiveSchoolId))
                .map(bookMapper::toDto)
                .orElseThrow(
                        () -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));
    }

    private static boolean hasDidactiekGenre(Book book) {
        if (book.getGenres() == null) {
            return false;
        }
        return book.getGenres().stream().anyMatch(genre -> {
            String name = genre.getNaam();
            if (name != null && name.toLowerCase(Locale.ROOT).contains("didactiek")) {
                return true;
            }
            return genre.getParent() != null
                    && genre.getParent().getNaam() != null
                    && genre.getParent().getNaam().toLowerCase(Locale.ROOT).contains("didactiek");
        });
    }
}
