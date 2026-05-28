package com.example.demo.controllers;

import com.example.demo.dto.CreateLoanRequest;
import com.example.demo.dto.LoanConditionOverviewDto;
import com.example.demo.dto.LoanDto;
import com.example.demo.dto.LoanExtensionRequestDto;
import com.example.demo.dto.ExtensionRequestTicket;
import com.example.demo.dto.ReturnLoanRequest;
import com.example.demo.dto.UpdateDueDateRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.DisplayNameResolver;
import com.example.demo.services.LoanService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.demo.exception.ApiException;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/uitleningen")
public class LoanController {

    private static final Logger logger = LoggerFactory.getLogger(LoanController.class);
    private final LoanService loanService;
    private final AppUserRepository appUserRepository;
    private final DisplayNameResolver displayNameResolver;

    public LoanController(LoanService loanService,
            AppUserRepository appUserRepository,
            DisplayNameResolver displayNameResolver) {
        this.loanService = loanService;
        this.appUserRepository = appUserRepository;
        this.displayNameResolver = displayNameResolver;
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<LoanDto> createLoan(
            @Valid @RequestBody CreateLoanRequest request,
            Authentication authentication) {
        try {
            logger.info("Creating loan for bookId={}, userSub={}, dueDate={}",
                    request.getBookId(), request.getUserSub(), request.getDueDate());
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoan(request, authentication.getName()));
        } catch (IllegalArgumentException e) {
            logger.warn("Loan creation validation error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (IllegalStateException e) {
            logger.warn("Loan creation conflict: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Database error creating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            logger.error("Unexpected error creating loan", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/bulk")
    public ResponseEntity<List<LoanDto>> createLoans(
            @Valid @RequestBody List<CreateLoanRequest> requests,
            Authentication authentication) {
        try {
            if (requests == null || requests.isEmpty()) {
                return ResponseEntity.badRequest().body(List.of());
            }
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(loanService.createLoans(requests, authentication.getName()));
        } catch (IllegalArgumentException e) {
            logger.warn("Bulk loan validation error: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().build();
        } catch (IllegalStateException e) {
            logger.warn("Bulk loan creation conflict: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        } catch (org.springframework.dao.DataAccessException e) {
            logger.error("Database error creating bulk loans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        } catch (RuntimeException e) {
            logger.error("Unexpected error creating bulk loans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
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

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/gebruiker/{sub}")
    public List<LoanDto> getActiveLoans(@PathVariable("sub") String userSub) {
        return loanService.getActiveLoansForUser(userSub);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/gebruiker/{sub}/historiek")
    public List<LoanDto> getLoanHistory(@PathVariable("sub") String userSub) {
        return loanService.getLoanHistoryForUser(userSub);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mijn")
    public List<LoanDto> getMyActiveLoans(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }
        return loanService.getActiveLoansForUser(authentication.getName());
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/mijn/historiek")
    public List<LoanDto> getMyLoanHistory(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }
        return loanService.getLoanHistoryForUser(authentication.getName());
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<LoanDto>> getLoansForBook(@PathVariable Long bookId) {
        return ResponseEntity.ok(loanService.getActiveLoansForBook(bookId));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/inspectie/conditie")
    public ResponseEntity<LoanConditionOverviewDto> getConditionOverview(Authentication authentication) {
        LoanConditionOverviewDto overview = loanService.getConditionOverview();

        String currentSub = authentication != null ? authentication.getName() : null;
        AppUser currentUser = currentSub == null ? null : appUserRepository.findBySub(currentSub).orElse(null);
        Long schoolId = (currentUser != null && currentUser.getSchool() != null
                && !"SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()))
                        ? currentUser.getSchool().getId()
                        : null;

        if (overview.getWorsenedReturns() != null && !overview.getWorsenedReturns().isEmpty()) {
            List<String> subs = overview.getWorsenedReturns().stream()
                    .map(LoanConditionOverviewDto.WorsenedReturnDto::getUserSub)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());
            Map<String, String> names = displayNameResolver.resolveAll(schoolId, subs);
            overview.getWorsenedReturns().forEach(row -> row.setUserDisplayName(
                    names.getOrDefault(row.getUserSub(), row.getUserSub())));
        }

        return ResponseEntity.ok(overview);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/all-active")
    public ResponseEntity<List<LoanDto>> getAllActiveLoans(Authentication authentication) {
        List<LoanDto> loans = loanService.getAllActiveLoans();

        String currentSub = authentication != null ? authentication.getName() : null;
        AppUser currentUser = currentSub == null ? null : appUserRepository.findBySub(currentSub).orElse(null);
        Long schoolId = (currentUser != null && currentUser.getSchool() != null
                && !"SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole()))
                        ? currentUser.getSchool().getId()
                        : null;

        List<String> subs = loans.stream()
                .map(LoanDto::getUserSub)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<String, String> names = displayNameResolver.resolveAll(schoolId, subs);
        loans.forEach(loan -> loan.setUserDisplayName(
                names.getOrDefault(loan.getUserSub(), loan.getUserSub())));

        return ResponseEntity.ok(loans);
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

    @PreAuthorize("isAuthenticated()") // Allow any authenticated user to send a message
    @PostMapping("/{id}/verlenging-aanvragen")
    public ResponseEntity<Void> createExtensionRequest(@PathVariable Long id,
            @RequestBody ExtensionRequestTicket request) {
        try {
            loanService.createExtensionRequest(id, request);
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/verlenging-aanvragen/openstaand")
    public ResponseEntity<List<LoanExtensionRequestDto>> getPendingExtensionRequests(Authentication authentication) {
        AppUser user = appUserRepository.findBySub(authentication.getName())
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.FORBIDDEN, "USER_NOT_FOUND"));

        if (user.getSchool() == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(loanService.getPendingExtensionRequests(user.getSchool().getId()));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/verlenging-aanvragen/count")
    public ResponseEntity<Long> getPendingExtensionRequestsCount(Authentication authentication) {
        AppUser user = appUserRepository.findBySub(authentication.getName())
                .orElseThrow(() -> new ApiException("Gebruiker niet gevonden", HttpStatus.FORBIDDEN, "USER_NOT_FOUND"));

        if (user.getSchool() == null) {
            return ResponseEntity.ok(0L);
        }

        return ResponseEntity.ok(loanService.getPendingExtensionRequestsCount(user.getSchool().getId()));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/verlenging-aanvragen/{requestId}/goedkeuren")
    public ResponseEntity<Void> approveExtensionRequest(@PathVariable Long requestId, Authentication authentication) {
        loanService.approveExtensionRequest(requestId, authentication.getName());
        return ResponseEntity.ok().build();
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/verlenging-aanvragen/{requestId}/afwijzen")
    public ResponseEntity<Void> rejectExtensionRequest(@PathVariable Long requestId,
            @RequestBody Map<String, String> body, Authentication authentication) {
        String notes = body.getOrDefault("notes", "");
        loanService.rejectExtensionRequest(requestId, authentication.getName(), notes);
        return ResponseEntity.ok().build();
    }
}