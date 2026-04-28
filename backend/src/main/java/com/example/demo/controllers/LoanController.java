package com.example.demo.controllers;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.dto.ReturnLoanRequest;
import com.example.demo.dto.UpdateDueDateRequest;
import com.example.demo.services.LoanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

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

    private boolean isLibrarian(String userRole) {
        return userRole != null && "bibbeheerder".equalsIgnoreCase(userRole);
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
            @RequestBody(required = false) ReturnLoanRequest request,
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!canLoan(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        try {
            return ResponseEntity.ok(loanService.returnLoan(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/gebruiker/{sub}")
    public List<LoanDto> getActiveLoans(@PathVariable("sub") String userSub) {
        return loanService.getActiveLoansForUser(userSub);
    }

    @GetMapping("/gebruiker/{sub}/historiek")
    public List<LoanDto> getLoanHistory(@PathVariable("sub") String userSub) {
        return loanService.getLoanHistoryForUser(userSub);
    }

    @GetMapping("/mijn/historiek")
    public List<LoanDto> getMyLoanHistory(
            @RequestHeader(value = "X-User-Sub", required = false) String userSubHeader) {
        String userSub = userSubHeader != null ? userSubHeader.trim() : "";
        if (userSub.isEmpty()) {
            return List.of();
        }
        return loanService.getLoanHistoryForUser(userSub);
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

    @GetMapping("/inspectie/conditie")
    public ResponseEntity<LoanConditionOverviewDto> getConditionOverview(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!isLibrarian(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(loanService.getConditionOverview());
    }

    @GetMapping("/all-active")
    public ResponseEntity<List<LoanDto>> getAllActiveLoans(
            @RequestHeader(value = "X-User-Role", required = false) String userRole) {
        if (!isLibrarian(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(loanService.getAllActiveLoans());
    }

    @PatchMapping("/{id}/due-date")
    public ResponseEntity<Void> updateLoanDueDate(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDueDateRequest request) {

        try {
            loanService.updateDueDate(id, request.getDueDate());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

}