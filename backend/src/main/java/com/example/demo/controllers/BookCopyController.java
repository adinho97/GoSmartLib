package com.example.demo.controllers;

import com.example.demo.dto.CopyDto;
import com.example.demo.dto.UpdateCopyStateRequest;
import com.example.demo.entities.Book;
import com.example.demo.entities.BookCopy;
import com.example.demo.repositories.BookCopyRepository;
import com.example.demo.repositories.BookRepository;

import com.example.demo.repositories.LoanRepository;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/exemplaren")
public class BookCopyController {

    private static final List<String> LOAN_ROLES = List.of("leerkracht", "bibbeheerder");
    private final BookCopyRepository copyRepo;
    private final BookRepository bookRepo;
    private final LoanRepository loanRepository;

    public BookCopyController(BookCopyRepository copyRepo, BookRepository bookRepo, LoanRepository loanRepository) {
        this.copyRepo = copyRepo;
        this.bookRepo = bookRepo;
        this.loanRepository = loanRepository;
    }

    private boolean canLoan(String userRole) {
        return userRole != null && LOAN_ROLES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(userRole));
    }

    @GetMapping("/boek/{bookId}/summary")
    public ResponseEntity<Map<String, Long>> getSummary(@PathVariable Long bookId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        long total = copyRepo.countByBook_Id(resolvedBookId);
        long available = copyRepo.findByBook_Id(resolvedBookId).stream()
            .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
            .count();
        return ResponseEntity.ok(Map.of("total", total, "available", available));
    }

    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<CopyDto>> getCopies(@PathVariable Long bookId) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        List<CopyDto> dtos = copyRepo.findByBook_Id(resolvedBookId).stream()
                .map(c -> {
                    CopyDto dto = new CopyDto();
                    dto.setId(c.getId());
                    dto.setStatus(c.getStatus());
                    dto.setCondition(c.getCondition());
                    return dto;
                }).collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping("/boek/{bookId}")
    public ResponseEntity<CopyDto> addCopy(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        Book book = bookRepo.findById(resolvedBookId).orElse(null);
        if (book == null)
            return ResponseEntity.notFound().build();

        BookCopy copy = new BookCopy();
        copy.setBook(book);
        copy.setStatus(BookCopy.CopyStatus.AVAILABLE);
        copy.setCondition(BookCopy.CopyCondition.GOOD);
        BookCopy saved = copyRepo.save(copy);

        CopyDto dto = new CopyDto();
        dto.setId(saved.getId());
        dto.setStatus(saved.getStatus());
        dto.setCondition(saved.getCondition());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PatchMapping("/{id}")
    public ResponseEntity<CopyDto> updateCopyState(
            @PathVariable Long id,
            @RequestBody UpdateCopyStateRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long resolvedId = Objects.requireNonNull(id, "id is required");
        BookCopy copy = copyRepo.findById(resolvedId).orElse(null);
        if (copy == null) {
            return ResponseEntity.notFound().build();
        }

        if (copy.getStatus() == BookCopy.CopyStatus.LOANED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (request == null || request.getStatus() == null || request.getCondition() == null) {
            return ResponseEntity.badRequest().build();
        }

        if (request.getStatus() == BookCopy.CopyStatus.LOANED) {
            return ResponseEntity.badRequest().build();
        }

        copy.setStatus(request.getStatus());
        copy.setCondition(request.getCondition());
        BookCopy updated = copyRepo.save(copy);

        CopyDto dto = new CopyDto();
        dto.setId(updated.getId());
        dto.setStatus(updated.getStatus());
        dto.setCondition(updated.getCondition());
        return ResponseEntity.ok(dto);
    }

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCopy(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long resolvedId = Objects.requireNonNull(id, "id is required");
        BookCopy copy = copyRepo.findById(resolvedId).orElse(null);
        if (copy == null)
            return ResponseEntity.notFound().build();

        // Blokkeer als het exemplaar momenteel uitgeleend is
        if (copy.getStatus() == BookCopy.CopyStatus.LOANED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Verwijder eerst alle loan records gekoppeld aan dit exemplaar
        loanRepository.deleteByCopy_Id(resolvedId);

        copyRepo.deleteById(resolvedId);
        return ResponseEntity.noContent().build();
    }
}