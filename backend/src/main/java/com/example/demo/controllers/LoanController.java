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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/uitleningen")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);
    private final LoanService loanService;

    public LoanController(LoanService loanService) {
        this.loanService = loanService;
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<LoanDto> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String lenderSub) {
        try {
            logger.info("Creating loan for bookId={}, userSub={}, dueDate={}",
                    request.getBookId(), request.getUserSub(), request.getDueDate());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoan(request, lenderSub));
        } catch (IllegalArgumentException e) {
            logger.warn("Loan creation validation error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            logger.warn("Loan creation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Unexpected error creating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/bulk")
    public ResponseEntity<List<LoanDto>> createLoans(
            @Valid @RequestBody List<CreateLoanRequest> requests,
            @RequestHeader(value = "X-User-Sub", required = false) String lenderSub) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(List.of());
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoans(requests, lenderSub));
        } catch (IllegalArgumentException e) {
            logger.warn("Bulk loan validation error: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Bulk loan creation conflict: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (Exception e) {
            logger.error("Unexpected error creating bulk loans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}/teruggeven")
    public ResponseEntity<LoanDto> returnLoan(
            @PathVariable Long id,
            @RequestBody(required = false) ReturnLoanRequest request) {
        try {
            return ResponseEntity.ok(loanService.returnLoan(id, request));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/gebruiker/{sub}")
    public List<LoanDto> getActiveLoans(@PathVariable("sub") String userSub) {
        return loanService.getActiveLoansForUser(userSub);
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
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

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<LoanDto>> getLoansForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(loanService.getActiveLoansForBook(bookId));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/inspectie/conditie")
    public ResponseEntity<LoanConditionOverviewDto> getConditionOverview() {
        return ResponseEntity.ok(loanService.getConditionOverview());
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/all-active")
    public ResponseEntity<List<LoanDto>> getAllActiveLoans() {
        return ResponseEntity.ok(loanService.getAllActiveLoans());
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
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