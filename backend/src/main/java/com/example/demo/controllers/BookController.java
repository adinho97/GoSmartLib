package com.example.demo.controllers;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.dto.CreateReviewRequest;
import com.example.demo.dto.LestipDto;
import com.example.demo.dto.PagedBookResponse;
import com.example.demo.dto.ReviewDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.dto.UpdateReviewRequest;
import com.example.demo.dto.ImportByIsbnRequest;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.BookDeletionService;
import com.example.demo.services.BookImportService;
import com.example.demo.services.BookLookupService;
import com.example.demo.services.BookQueryService;
import com.example.demo.services.BookStatsService;
import com.example.demo.services.BookWriteService;
import com.example.demo.services.LestipService;
import com.example.demo.services.ReviewContext;
import com.example.demo.services.ReviewService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/boeken")
@SuppressWarnings("null")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);

    private final AppUserRepository appUserRepository;
    private final BookLookupService bookLookupService;
    private final BookImportService bookImportService;
    private final BookStatsService bookStatsService;
    private final BookDeletionService bookDeletionService;
    private final BookQueryService bookQueryService;
    private final BookWriteService bookWriteService;
    private final ReviewService reviewService;
    private final LestipService lestipService;

    public BookController(AppUserRepository appUserRepository,
            BookLookupService bookLookupService,
            BookImportService bookImportService,
            BookStatsService bookStatsService,
            BookDeletionService bookDeletionService,
            BookQueryService bookQueryService,
            BookWriteService bookWriteService,
            ReviewService reviewService,
            LestipService lestipService) {
        this.appUserRepository = appUserRepository;
        this.bookLookupService = bookLookupService;
        this.bookImportService = bookImportService;
        this.bookStatsService = bookStatsService;
        this.bookDeletionService = bookDeletionService;
        this.bookQueryService = bookQueryService;
        this.bookWriteService = bookWriteService;
        this.reviewService = reviewService;
        this.lestipService = lestipService;
    }

    @GetMapping
    public List<BookDto> getAll(
            @RequestParam(required = false) Long schoolId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication, subHeader);
        boolean excludeDidactic = isStudentRole(authentication, roleHeader);
        return bookQueryService.listAll(effectiveSchoolId, excludeDidactic);
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/didactisch")
    public List<BookDto> getDidacticCollection(
            @RequestParam(required = false) Long schoolId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication, subHeader);
        return bookQueryService.listDidacticCollection(effectiveSchoolId);
    }

    @GetMapping("/paged")
    public ResponseEntity<PagedBookResponse> getPaged(
            @RequestParam(required = false) Long schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String query,
            Authentication authentication,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication, subHeader);
        boolean excludeDidactic = isStudentRole(authentication, roleHeader);
        return ResponseEntity.ok(
                bookQueryService.searchPaged(effectiveSchoolId, query, excludeDidactic, page, size));
    }

    @GetMapping("/stats")
    public List<BookDto> getStatsOrderedByPopularity() {
        logger.info("Fetching books with loan statistics, sorted by popularity");
        return bookStatsService.getBooksWithStats();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable @NonNull Long id,
            @RequestParam(required = false) Long schoolId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication, subHeader);
        boolean excludeDidactic = isStudentRole(authentication, roleHeader);
        return ResponseEntity.ok(bookQueryService.getBook(id, effectiveSchoolId, excludeDidactic));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody BookDto bookDto) {
        return ResponseEntity.ok(bookWriteService.create(bookDto));
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> getByIsbn(@PathVariable @NonNull String isbn,
            @RequestParam(required = false) Long schoolId,
            Authentication authentication) {
        try {
            Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication);
            return bookLookupService.findByIsbn(isbn, effectiveSchoolId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/go/{goNumber}")
    public ResponseEntity<BookDto> getByGoNumber(@PathVariable @NonNull String goNumber,
            @RequestParam(required = false) Long schoolId,
            Authentication authentication) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, authentication);
        return ResponseEntity.ok(bookQueryService.getBookByGoNumber(goNumber, effectiveSchoolId));
    }

    @GetMapping("/preview/{isbn}")
    public ResponseEntity<BookDto> previewByIsbn(@PathVariable @NonNull String isbn) {
        BookDto dto;
        try {
            dto = bookLookupService.fetchPreviewByIsbn(isbn);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> importByIsbn(@PathVariable @NonNull String isbn,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false, defaultValue = "false") boolean isDidactisch,
            @RequestBody(required = false) ImportByIsbnRequest request) {
        BookDto dto = null;
        try {
            int quantity = 1;
            if (request != null && request.getCopyQuantity() != null) {
                quantity = request.getCopyQuantity();
            }
            
            for (int i = 0; i < quantity; i++) {
                dto = bookImportService.importByIsbn(isbn, schoolId, isDidactisch);
                if (i == quantity - 1) {
                    return ResponseEntity.ok(dto);
                }
            }
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/isbn/bulk")
    public ResponseEntity<ImportResultDto> importBulkByIsbn(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long schoolId,
            @RequestParam(required = false) String defaultCondition,
            @RequestParam(required = false) String booksConfig,
            @RequestParam(required = false, defaultValue = "false") boolean isDidactisch) {
        try {
            ImportResultDto result = bookImportService.importBulkByIsbn(file, schoolId, defaultCondition, booksConfig, isDidactisch);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader) {
        if (!isLibrarianOrAdmin(authentication, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long librarianSchoolId = resolveEffectiveSchoolId(null, authentication, subHeader);
        bookDeletionService.deleteBook(id, librarianSchoolId);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<BookDto> update(@PathVariable @NonNull Long id, @Valid @RequestBody BookDto bookDto) {
        return ResponseEntity.ok(bookWriteService.update(id, bookDto));
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/{id}/lestip")
    public ResponseEntity<LestipDto> getLestip(@PathVariable @NonNull Long id,
            @RequestParam(required = false, defaultValue = "all") String scope,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (!hasAnyLestipRole(authentication, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(lestipService.getLestip(id, scope, userName));
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}/lestip")
    public ResponseEntity<LestipDto> updateLestip(@PathVariable @NonNull Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @Valid @RequestBody UpdateLestipRequest request) {
        if (!hasAnyLestipRole(authentication, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(lestipService.updateLestip(id, request, userName));
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}/lestip")
    public ResponseEntity<Void> deleteLestip(@PathVariable @NonNull Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (!hasAnyLestipRole(authentication, roleHeader)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        lestipService.deleteLestip(id, userName);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable @NonNull Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader) {
        return ResponseEntity.ok(
                reviewService.listReviews(id, buildReviewContext(authentication, roleHeader, subHeader)));
    }

    @GetMapping("/reviews/mijn/aantal")
    public ResponseEntity<Map<String, Long>> getMyReviewCount(Authentication authentication) {
        String reviewerUserSub = authentication != null ? authentication.getName() : null;
        long count = reviewService.countByCurrentUser(reviewerUserSub);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> createReview(@PathVariable @NonNull Long id,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader,
            @Valid @RequestBody CreateReviewRequest request) {
        ReviewDto dto = reviewService.createReview(id, request,
                buildReviewContext(authentication, roleHeader, subHeader));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable @NonNull Long bookId,
            @PathVariable @NonNull Long reviewId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader) {
        reviewService.deleteReview(bookId, reviewId,
                buildReviewContext(authentication, roleHeader, subHeader));
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(@PathVariable @NonNull Long bookId,
            @PathVariable @NonNull Long reviewId,
            Authentication authentication,
            @RequestHeader(value = "X-User-Role", required = false) String roleHeader,
            @RequestHeader(value = "X-User-Sub", required = false) String subHeader,
            @Valid @RequestBody UpdateReviewRequest request) {
        ReviewDto dto = reviewService.updateReview(bookId, reviewId, request,
                buildReviewContext(authentication, roleHeader, subHeader));
        return ResponseEntity.ok(dto);
    }

    // ---- auth / context helpers ---------------------------------------------

    private ReviewContext buildReviewContext(Authentication authentication, String roleHeader, String subHeader) {
        String userSub = resolveUserSub(authentication, subHeader, roleHeader);
        boolean isLibrarian = isLibrarianOrAdmin(authentication, roleHeader);
        Long leerlingSchoolId = isStudentRole(authentication, roleHeader)
                ? resolveEffectiveSchoolId(null, authentication, subHeader)
                : null;
        return new ReviewContext(userSub, isLibrarian, leerlingSchoolId);
    }

    private boolean hasAnyLestipRole(Authentication authentication, String roleHeader) {
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean matches = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority() == null ? "" : auth.getAuthority())
                    .anyMatch(authority -> "ROLE_LEERKRACHT".equalsIgnoreCase(authority)
                            || "ROLE_BIBBEHEERDER".equalsIgnoreCase(authority)
                            || "ROLE_SUPER_ADMIN".equalsIgnoreCase(authority));
            if (matches) {
                return true;
            }
        }

        String normalizedRole = normalizeRole(roleHeader);
        return "leerkracht".equals(normalizedRole)
                || "bibbeheerder".equals(normalizedRole)
                || "super_admin".equals(normalizedRole);
    }

    private String normalizeRole(String role) {
        if (!StringUtils.hasText(role)) {
            return null;
        }
        String trimmed = role.trim();
        if (trimmed.regionMatches(true, 0, "ROLE_", 0, 5)) {
            trimmed = trimmed.substring(5);
        } else if (trimmed.regionMatches(true, 0, "ROLE:", 0, 5)) {
            trimmed = trimmed.substring(5);
        }
        trimmed = trimmed.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }

    private boolean isLibrarianOrAdmin(Authentication authentication, String roleHeader) {
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean matches = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority() == null ? "" : auth.getAuthority())
                    .anyMatch(authority -> "ROLE_BIBBEHEERDER".equalsIgnoreCase(authority)
                            || "ROLE_SUPER_ADMIN".equalsIgnoreCase(authority));
            if (matches) {
                return true;
            }
        }
        String normalizedRole = normalizeRole(roleHeader);
        return "bibbeheerder".equals(normalizedRole) || "super_admin".equals(normalizedRole);
    }

    private boolean isStudentRole(Authentication authentication, String roleHeader) {
        if (authentication != null && authentication.getAuthorities() != null) {
            boolean matches = authentication.getAuthorities().stream()
                    .map(auth -> auth.getAuthority() == null ? "" : auth.getAuthority())
                    .anyMatch(authority -> "ROLE_LEERLING".equalsIgnoreCase(authority));
            if (matches) {
                return true;
            }
        }
        return "leerling".equals(normalizeRole(roleHeader));
    }

    private Long resolveEffectiveSchoolId(Long requestedSchoolId, Authentication authentication) {
        boolean studentCaller = isStudentRole(authentication, null);
        if (requestedSchoolId != null && !studentCaller) {
            return requestedSchoolId;
        }
        if (authentication == null) {
            return null;
        }
        String sub = authentication.getName();
        if (!StringUtils.hasText(sub)) {
            return null;
        }
        return appUserRepository.findBySub(sub.trim())
                .filter(user -> user.getSchool() != null)
                .map(user -> Objects.requireNonNull(user.getSchool().getId(), "schoolId is required"))
                .orElse(null);
    }

    private Long resolveEffectiveSchoolId(Long requestedSchoolId, Authentication authentication, String subHeader) {
        boolean studentCaller = isStudentRole(authentication, null);
        if (requestedSchoolId != null && !studentCaller) {
            return requestedSchoolId;
        }

        String sub = resolveUserSub(authentication, subHeader, null);
        if (!StringUtils.hasText(sub)) {
            return null;
        }

        return appUserRepository.findBySub(sub.trim())
                .filter(user -> user.getSchool() != null)
                .map(user -> Objects.requireNonNull(user.getSchool().getId(), "schoolId is required"))
                .orElse(null);
    }

    private String resolveUserSub(Authentication authentication, String subHeader, String roleHeader) {
        if (authentication != null && StringUtils.hasText(authentication.getName())) {
            return authentication.getName().trim();
        }
        if (StringUtils.hasText(subHeader)) {
            return subHeader.trim();
        }
        String normalizedRole = normalizeRole(roleHeader);
        if (StringUtils.hasText(normalizedRole)) {
            return "role:" + normalizedRole;
        }
        return null;
    }
}
