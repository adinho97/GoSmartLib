package com.example.demo.controllers;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanDto;
import com.example.demo.services.LoanService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uitleningen")
public class LoanController {

    private static final List<String> LOAN_ROLES = List.of("leerkracht", "bibbeheerder");
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    private boolean canLoan(String userRole) {
        return userRole != null && LOAN_ROLES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(userRole));
    }

    @PostMapping
    public ResponseEntity<LoanDto> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoan(request));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    @PutMapping("/{id}/teruggeven")
    public ResponseEntity<LoanDto> returnLoan(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(loanService.returnLoan(id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/gebruiker/{username}")
    public List<LoanDto> getActiveLoans(@PathVariable String username) {
        return loanService.getActiveLoansForUser(username);
    }

    @GetMapping("/gebruiker/{username}/historiek")
    public List<LoanDto> getLoanHistory(@PathVariable String username) {
        return loanService.getLoanHistoryForUser(username);
    }

    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<LoanDto>> getLoansForBook(
            @PathVariable Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(loanService.getActiveLoansForBook(bookId));
    }
}