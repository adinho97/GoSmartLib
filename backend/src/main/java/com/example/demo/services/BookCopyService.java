package com.example.demo.services;

import com.example.demo.dto.CopyDto;
import com.example.demo.dto.UpdateCopyStateRequest;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.LoanRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BookCopyService {

    private final BookCopyRepository bookCopyRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;

    public BookCopyService(BookCopyRepository bookCopyRepository,
            BookRepository bookRepository,
            LoanRepository loanRepository) {
        this.bookCopyRepository = bookCopyRepository;
        this.bookRepository = bookRepository;
        this.loanRepository = loanRepository;
    }

    public Map<String, Long> getSummary(Long bookId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        long total = bookCopyRepository.countByBook_Id(resolvedBookId);
        long available = bookCopyRepository.findByBook_Id(resolvedBookId).stream()
                .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                        || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
                .count();
        return Map.of("total", total, "available", available);
    }

    public List<CopyDto> getCopiesForBook(Long bookId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        return bookCopyRepository.findByBook_Id(resolvedBookId).stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public CopyDto addCopy(Long bookId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        Book book = bookRepository.findById(resolvedBookId)
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
        copy.setCondition(BookCopy.CopyCondition.GOOD);
        BookCopy saved = bookCopyRepository.save(copy);
        return toDto(saved);
    }

    public CopyDto updateCopyState(Long id, UpdateCopyStateRequest request) {
        Long resolvedId = Objects.requireNonNull(id, "id is required");
        if (request == null || request.getStatus() == null || request.getCondition() == null) {
            throw new ApiException("Status en conditie zijn verplicht", HttpStatus.BAD_REQUEST, "INVALID_REQUEST");
        }
        if (request.getStatus() == BookCopy.CopyStatus.LOANED) {
            throw new ApiException("Status LOANED is niet toegestaan via deze endpoint", HttpStatus.BAD_REQUEST,
                    "INVALID_STATUS");
        }

        BookCopy copy = bookCopyRepository.findById(resolvedId)
                .orElseThrow(() -> new ApiException("Exemplaar niet gevonden", HttpStatus.NOT_FOUND, "COPY_NOT_FOUND"));

        if (copy.getStatus() == BookCopy.CopyStatus.LOANED) {
            throw new ApiException("Kan exemplaar niet wijzigen: momenteel uitgeleend", HttpStatus.CONFLICT,
                    "COPY_LOANED");
        }

        copy.setStatus(request.getStatus());
        copy.setCondition(request.getCondition());
        BookCopy updated = bookCopyRepository.save(copy);
        return toDto(updated);
    }

    @Transactional
    public void deleteCopy(Long id) {
        Long resolvedId = Objects.requireNonNull(id, "id is required");
        BookCopy copy = bookCopyRepository.findById(resolvedId)
                .orElseThrow(() -> new ApiException("Exemplaar niet gevonden", HttpStatus.NOT_FOUND, "COPY_NOT_FOUND"));

        if (copy.getStatus() == BookCopy.CopyStatus.LOANED) {
            throw new ApiException("Kan exemplaar niet verwijderen: momenteel uitgeleend", HttpStatus.CONFLICT,
                    "COPY_LOANED");
        }

        loanRepository.deleteByCopy_Id(resolvedId);
        bookCopyRepository.deleteById(resolvedId);
    }

    private CopyDto toDto(BookCopy copy) {
        CopyDto dto = new CopyDto();
        dto.setId(copy.getId());
        dto.setStatus(copy.getStatus());
        dto.setCondition(copy.getCondition());
        return dto;
    }
}
