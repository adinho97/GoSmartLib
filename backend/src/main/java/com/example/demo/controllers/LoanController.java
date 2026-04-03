package com.example.demo.controllers;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanDto;
import com.example.demo.services.LoanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uitleningen")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);
    private static final List<String> LOAN_ROLES = List.of("leerkracht", "bibbeheerder");
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    private boolean canLoan(String userRole) {
        return userRole != null && LOAN_ROLES.stream()
                .anyMatch(r -> r.equalsIgnoreCase(userRole));
    }

    private boolean isAdmin(String userRole) {
        return userRole != null && "bibbeheerder".equalsIgnoreCase(userRole);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isSelf(String requestedSub, String currentSub) {
        return !requestedSub.isEmpty() && requestedSub.equals(currentSub);
    }

    @PostMapping
    public ResponseEntity<LoanDto> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            logger.info("Creating loan for bookId={}, userSub={}, dueDate={}",
                    request.getBookId(), request.getUserSub(), request.getDueDate());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoan(request));
        } catch (IllegalStateException e) {
            logger.warn("Loan creation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Unexpected error creating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
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

    @GetMapping("/gebruiker/{sub}")
    public ResponseEntity<List<LoanDto>> getActiveLoans(
            @PathVariable("sub") String userSub,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String currentUserSubHeader) {
        String requestedSub = normalize(userSub);
        String currentUserSub = normalize(currentUserSubHeader);

        if (currentUserSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!canLoan(userRole) && !isSelf(requestedSub, currentUserSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(loanService.getActiveLoansForUser(requestedSub));
    }

    @GetMapping("/gebruiker/{sub}/historiek")
    public ResponseEntity<List<LoanDto>> getLoanHistory(
            @PathVariable("sub") String userSub,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String currentUserSubHeader) {
        String requestedSub = normalize(userSub);
        String currentUserSub = normalize(currentUserSubHeader);

        if (currentUserSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!isAdmin(userRole) && !isSelf(requestedSub, currentUserSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        return ResponseEntity.ok(loanService.getLoanHistoryForUser(requestedSub));
    }

    @GetMapping("/mijn/historiek")
    public ResponseEntity<List<LoanDto>> getMyLoanHistory(
            @RequestHeader(value = "X-User-Sub", required = false) String currentUserSubHeader) {
        String currentUserSub = normalize(currentUserSubHeader);
        if (currentUserSub.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(loanService.getLoanHistoryForUser(currentUserSub));
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