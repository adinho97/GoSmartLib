package com.example.demo.controllers;

import com.example.demo.dto.CopyDto;
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
        long total = copyRepo.countByBook_Id(bookId);
        long available = copyRepo.findByBook_Id(bookId).stream()
            .filter(c -> c.getStatus() == BookCopy.CopyStatus.AVAILABLE
                || c.getStatus() == BookCopy.CopyStatus.DAMAGED)
            .count();
        return ResponseEntity.ok(Map.of("total", total, "available", available));
    }

    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<CopyDto>> getCopies(@PathVariable Long bookId) {
        List<CopyDto> dtos = copyRepo.findByBook_Id(bookId).stream()
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
        Book book = bookRepo.findById(bookId).orElse(null);
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
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCopy(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        BookCopy copy = copyRepo.findById(id).orElse(null);
        if (copy == null)
            return ResponseEntity.notFound().build();

        // Blokkeer als het exemplaar momenteel uitgeleend is
        if (copy.getStatus() == BookCopy.CopyStatus.LOANED) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        // Verwijder eerst alle loan records gekoppeld aan dit exemplaar
        loanRepository.deleteByCopy_Id(id);

        copyRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}