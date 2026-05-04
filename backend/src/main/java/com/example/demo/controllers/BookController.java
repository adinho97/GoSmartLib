package com.example.demo.controllers;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.dto.CreateReviewRequest;
import com.example.demo.dto.LestipDto;
import com.example.demo.dto.PagedBookResponse;
import com.example.demo.dto.ReviewDto;
import com.example.demo.dto.UpdateLestipRequest;
import com.example.demo.dto.UpdateReviewRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Review;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.BookService;
import com.example.demo.config.AuthService;
import com.example.demo.services.ReviewModerationService;
import com.example.demo.services.SchoolService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.util.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.security.SecureRandom;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api/boeken")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private static final String LIBRARIAN_ROLE = "bibbeheerder";
    private static final String TEACHER_ROLE = "leerkracht";
    private static final String STUDENT_ROLE = "leerling";
    private static final SecureRandom GO_NUMBER_RANDOM = new SecureRandom();
    private final BookRepository repo;
    private final ReviewRepository reviewRepository;
    private final AppUserRepository appUserRepository;
    private final BookService bookService;
    private final SchoolService schoolService;
    private final ReviewModerationService reviewModerationService;
    private final AuthService authService;

    public BookController(BookRepository repo, ReviewRepository reviewRepository,
            AppUserRepository appUserRepository, BookService bookService,
            SchoolService schoolService, ReviewModerationService reviewModerationService,
            AuthService authService) {
        this.repo = repo;
        this.reviewRepository = reviewRepository;
        this.appUserRepository = appUserRepository;
        this.bookService = bookService;
        this.schoolService = schoolService;
        this.reviewModerationService = reviewModerationService;
        this.authService = authService;
    }

    @GetMapping
    public List<BookDto> getAll(
            @RequestParam(required = false) Long schoolId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, userRole, userSub);
        List<Book> books = effectiveSchoolId == null ? repo.findAll() : repo.findAllBySchool_Id(effectiveSchoolId);
        return books.stream().map(BookMapper::toDto).collect(Collectors.toList());
    }

            @GetMapping("/paged")
            public ResponseEntity<PagedBookResponse> getPaged(
                @RequestParam(required = false) Long schoolId,
                @RequestParam(defaultValue = "0") int page,
                    @RequestParam(defaultValue = "5") int size,
                    @RequestParam(required = false) String query,
                    @RequestHeader(value = "X-User-Role", required = false) String userRole,
                    @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
            int safePage = Math.max(page, 0);
            int safeSize = Math.max(size, 1);
                String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
                Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, userRole, userSub);

                Page<Book> books = repo.searchPaged(
                    effectiveSchoolId,
                    normalizedQuery,
                    PageRequest.of(safePage, safeSize));

            List<BookDto> items = books.stream()
                .map(BookMapper::toDto)
                .collect(Collectors.toList());
            return ResponseEntity.ok(new PagedBookResponse(items, books.getTotalElements()));
            }

    @GetMapping("/stats")
    public List<BookDto> getStatsOrderedByPopularity() {
        logger.info("Fetching books with loan statistics, sorted by popularity");
        return bookService.getBooksWithStats();
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable @NonNull Long id,
            @RequestParam(required = false) Long schoolId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, userRole, userSub);
        return (effectiveSchoolId == null ? repo.findById(id) : repo.findByIdAndSchool_Id(id, effectiveSchoolId))
                .map(BookMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<BookDto> create(@Valid @RequestBody BookDto bookDto) {
        logger.info("Creating book: titel={}, auteur={}, schoolId={}", bookDto.getTitel(), bookDto.getAuteur(),
                bookDto.getSchoolId());

        School school;
        try {
            school = schoolService.getByIdOrDefault(bookDto.getSchoolId());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            logger.error("School not found or invalid: {}", bookDto.getSchoolId(), ex);
            return ResponseEntity.badRequest().build();
        }

        if (bookDto.getIsbn() != null && !bookDto.getIsbn().trim().isEmpty()) {
            if (repo.findByIsbnAndSchool_Id(bookDto.getIsbn(), school.getId()).isPresent()) {
                return ResponseEntity.status(HttpStatus.CONFLICT).build();
            }
        }

        try {
            Book entity = BookMapper.toEntity(bookDto);
            entity.setId(null);
            entity.setGoNumber(null);
            entity.setSchool(school);
            assignGoNumberIfNeeded(entity);
            Book saved = repo.save(entity);
            logger.info("Book saved with id: {}", saved.getId());

            BookDto result = repo.findById(saved.getId())
                    .map(BookMapper::toDto)
                    .orElse(BookMapper.toDto(saved));

            return ResponseEntity.ok(result);
        } catch (Exception ex) {
            logger.error("Error creating book", ex);
            throw ex;
        }
    }

    @GetMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> getByIsbn(@PathVariable @NonNull String isbn,
            @RequestParam(required = false) Long schoolId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        try {
            Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, userRole, userSub);
            return bookService.findByIsbn(isbn, effectiveSchoolId)
                    .map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @GetMapping("/go/{goNumber}")
    public ResponseEntity<BookDto> getByGoNumber(@PathVariable @NonNull String goNumber,
            @RequestParam(required = false) Long schoolId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        String trimmedGoNumber = goNumber.trim().toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(trimmedGoNumber)) {
            return ResponseEntity.badRequest().build();
        }
        Long effectiveSchoolId = resolveEffectiveSchoolId(schoolId, userRole, userSub);
        return (effectiveSchoolId == null
                ? repo.findByGoNumber(trimmedGoNumber)
                : repo.findByGoNumberAndSchool_Id(trimmedGoNumber, effectiveSchoolId))
                .map(BookMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/preview/{isbn}")
    public ResponseEntity<BookDto> previewByIsbn(@PathVariable @NonNull String isbn) {
        BookDto dto;
        try {
            dto = bookService.fetchPreviewByIsbn(isbn);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/isbn/{isbn}")
    public ResponseEntity<BookDto> importByIsbn(@PathVariable @NonNull String isbn,
            @RequestParam(required = false) Long schoolId) {
        BookDto dto;
        try {
            dto = bookService.importByIsbn(isbn, schoolId);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }

        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/isbn/bulk")
    public ResponseEntity<ImportResultDto> importBulkByIsbn(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long schoolId) {
        try {
            ImportResultDto result = bookService.importBulkByIsbn(file, schoolId);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (!isLibrarian(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Long librarianSchoolId = resolveEffectiveSchoolId(null, userRole, userSub);
        boolean exists = librarianSchoolId == null ? repo.existsById(id) : repo.existsByIdAndSchool_Id(id, librarianSchoolId);
        if (!exists) {
            return ResponseEntity.notFound().build();
        }

        repo.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    public ResponseEntity<BookDto> update(@PathVariable @NonNull Long id, @Valid @RequestBody BookDto bookDto) {
        Book existing = repo.findById(id).orElse(null);
        if (existing == null) {
            return ResponseEntity.notFound().build();
        }

        existing.setTitel(bookDto.getTitel());
        existing.setAuteur(bookDto.getAuteur());
        existing.setIsbn(bookDto.getIsbn());
        existing.setCover(bookDto.getCover());
        existing.setBeschrijving(bookDto.getBeschrijving());
        existing.setGenre(bookDto.getGenre());
        existing.setUitgaveDatum(bookDto.getUitgaveDatum());
        existing.setPaginas(bookDto.getPaginas());
        existing.setTaal(bookDto.getTaal());
        existing.setUitgeverij(bookDto.getUitgeverij());
        existing.setLeesniveau(bookDto.getLeesniveau());
        assignGoNumberIfNeeded(existing);

        if (bookDto.getSchoolId() != null) {
            School school = schoolService.getByIdOrDefault(bookDto.getSchoolId());
            existing.setSchool(school);
        }

        repo.save(existing);

        return repo.findById(id)
                .map(BookMapper::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/lestip")
    public ResponseEntity<LestipDto> getLestip(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (!isTeacher(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Book book = repo.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        String currentUserSub = resolveUserSub(userSub, userName);
        return ResponseEntity.ok(toLestipDto(book, currentUserSub));
    }

    @PutMapping("/{id}/lestip")
    public ResponseEntity<LestipDto> updateLestip(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @Valid @RequestBody UpdateLestipRequest request) {
        if (!isTeacher(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String normalizedUserSub = resolveUserSub(userSub, userName);

        Book book = repo.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        if (hasLestip(book.getLestip())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        String normalizedLestip = normalizeLestip(request.getLestip());
        if (normalizedLestip == null) {
            return ResponseEntity.badRequest().build();
        }

        book.setLestip(normalizedLestip);
        book.setLestipAuteur(normalizedUserSub);

        Book savedBook = repo.save(book);
        return ResponseEntity.ok(toLestipDto(savedBook, normalizedUserSub));
    }

    @DeleteMapping("/{id}/lestip")
    public ResponseEntity<Void> deleteLestip(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (!isTeacher(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String normalizedUserSub = resolveUserSub(userSub, userName);

        Book book = repo.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        if (!hasLestip(book.getLestip())) {
            return ResponseEntity.notFound().build();
        }

        String lestipAuteur = book.getLestipAuteur();
        boolean hasStoredAuteur = StringUtils.hasText(lestipAuteur);
        if (hasStoredAuteur && (normalizedUserSub == null || !isSameUser(normalizedUserSub, lestipAuteur))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        book.setLestip(null);
        book.setLestipAuteur(null);
        repo.save(book);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        if (!isBookAccessibleToLeerling(id, userRole, userSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String normalizedUserSub = resolveReviewUserKey(userSub, userName, userRole);

        List<ReviewDto> reviews = reviewRepository.findByBook_IdOrderByCreatedAtDesc(id)
                .stream()
                .map(review -> toReviewDto(review, userRole, normalizedUserSub))
                .collect(Collectors.toList());

        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/reviews/mijn/aantal")
    public ResponseEntity<Map<String, Long>> getMyReviewCount(
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        String reviewerUserSub = resolveReviewUserKey(userSub, userName, userRole);
        if (!StringUtils.hasText(reviewerUserSub)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Long reviewerUserId = resolveReviewerUserId(userSub, userName);
        long count = reviewerUserId == null
                ? reviewRepository.countByReviewerUserSub(reviewerUserSub)
                : reviewRepository.countByReviewerUserSubOrReviewerUserId(reviewerUserSub, reviewerUserId);

        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> createReview(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @Valid @RequestBody CreateReviewRequest request) {
        Book book = repo.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        if (!isBookAccessibleToLeerling(id, userRole, userSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Review review = new Review();
        review.setBook(book);
        review.setRating(request.getRating());
        String trimmedComment = request.getComment().trim();
        reviewModerationService.validateReviewComment(trimmedComment);
        review.setComment(trimmedComment);
        boolean isAnonymous = Boolean.TRUE.equals(request.getAnonymous());
        String reviewerUserSub = resolveReviewUserKey(userSub, userName, userRole);
        Long reviewerUserId = isAnonymous ? null : resolveReviewerUserId(userSub, userName);
        review.setAnonymous(isAnonymous);

        if (reviewerUserSub == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        if (reviewerUserSub != null && reviewRepository.existsByBook_IdAndReviewerUserSub(id, reviewerUserSub)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (reviewerUserId != null && reviewRepository.existsByBook_IdAndReviewerUserId(id, reviewerUserId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        if (isAnonymous) {
            review.setReviewerUserId(null);
            review.setReviewerUserSub(reviewerUserSub);
        } else {
            review.setReviewerUserId(reviewerUserId);
            review.setReviewerUserSub(reviewerUserSub);
        }

        Review saved = reviewRepository.save(review);
        String normalizedUserSub = resolveReviewUserKey(userSub, userName, userRole);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(toReviewDto(saved, userRole, normalizedUserSub));
    }

    @DeleteMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable @NonNull Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @PathVariable @NonNull Long reviewId) {
        if (!repo.existsById(bookId)) {
            return ResponseEntity.notFound().build();
        }

        if (!isBookAccessibleToLeerling(bookId, userRole, userSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getBook() == null || !bookId.equals(review.getBook().getId())) {
            return ResponseEntity.notFound().build();
        }

        String normalizedUserSub = resolveReviewUserKey(userSub, userName, userRole);
        if (!canManageReview(review, userRole, normalizedUserSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<ReviewDto> updateReview(@PathVariable @NonNull Long bookId,
            @PathVariable @NonNull Long reviewId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName,
            @Valid @RequestBody UpdateReviewRequest request) {
        if (!repo.existsById(bookId)) {
            return ResponseEntity.notFound().build();
        }

        if (!isBookAccessibleToLeerling(bookId, userRole, userSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getBook() == null || !bookId.equals(review.getBook().getId())) {
            return ResponseEntity.notFound().build();
        }

        String normalizedUserSub = resolveReviewUserKey(userSub, userName, userRole);
        if (!canManageReview(review, userRole, normalizedUserSub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        String trimmedComment = request.getComment().trim();
        reviewModerationService.validateReviewComment(trimmedComment);
        review.setRating(request.getRating());
        review.setComment(trimmedComment);

        Review savedReview = reviewRepository.save(review);
        return ResponseEntity.ok(toReviewDto(savedReview, userRole, normalizedUserSub));
    }

    private ReviewDto toReviewDto(Review review, String userRole, String normalizedUserSub) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewerUserId(review.getReviewerUserId());
        dto.setReviewerUserName(resolveReviewerUserName(review));
        dto.setCanManage(canManageReview(review, userRole, normalizedUserSub));
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    private LestipDto toLestipDto(Book book, String currentUserSub) {
        LestipDto dto = new LestipDto();
        String lestipText = book.getLestip();
        String lestipAuteur = book.getLestipAuteur();

        dto.setLestip(lestipText == null ? "" : lestipText);
        dto.setAuteurNaam(lestipAuteur == null ? "" : lestipAuteur);
        boolean magVerwijderen = hasLestip(lestipText)
                && (!StringUtils.hasText(lestipAuteur)
                        || (currentUserSub != null && isSameUser(currentUserSub, lestipAuteur)));
        dto.setMagVerwijderen(magVerwijderen);
        return dto;
    }

    private String normalizeLestip(String lestip) {
        if (lestip == null) {
            return null;
        }

        String trimmedLestip = lestip.trim();
        if (trimmedLestip.isEmpty()) {
            return null;
        }

        return trimmedLestip;
    }

    private String normalizeUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }

        String trimmedUserName = userName.trim();
        if (trimmedUserName.isEmpty()) {
            return null;
        }

        return trimmedUserName;
    }

    private boolean isSameUser(String firstUserName, String secondUserName) {
        String normalizedFirst = canonicalUserName(firstUserName);
        String normalizedSecond = canonicalUserName(secondUserName);

        return normalizedFirst != null && normalizedFirst.equals(normalizedSecond);
    }

    private String canonicalUserName(String userName) {
        if (!StringUtils.hasText(userName)) {
            return null;
        }

        return userName
                .trim()
                .replaceAll("\\s+", " ")
                .toLowerCase(Locale.ROOT);
    }

    private String resolveUserSub(String userSub, String userName) {
        if (StringUtils.hasText(userSub)) {
            return userSub.trim();
        }
        return normalizeUserName(userName);
    }

    private String resolveReviewUserKey(String userSub, String userName, String userRole) {
        String resolvedUserSub = resolveUserSub(userSub, userName);
        if (StringUtils.hasText(resolvedUserSub)) {
            return resolvedUserSub;
        }

        if (StringUtils.hasText(userRole)) {
            return "role:" + userRole.trim().toLowerCase(Locale.ROOT);
        }

        return null;
    }

    private Long resolveReviewerUserId(String userSub, String userName) {
        if (StringUtils.hasText(userSub)) {
            String trimmed = userSub.trim();
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                // If it's not a numeric ID, try to resolve by sub into AppUser ID
                return appUserRepository.findBySub(trimmed)
                        .map(AppUser::getId)
                        .orElse(null);
            }
        }

        if (StringUtils.hasText(userName)) {
            try {
                return Long.parseLong(userName.trim());
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }

    private Long resolveCurrentUserId(String userSub) {
        if (StringUtils.hasText(userSub)) {
            String trimmed = userSub.trim();
            try {
                return Long.parseLong(trimmed);
            } catch (NumberFormatException ignored) {
                // If it's not a numeric ID, try to resolve by sub into AppUser ID
                return appUserRepository.findBySub(trimmed)
                        .map(AppUser::getId)
                        .orElse(null);
            }
        }
        return null;
    }

    private boolean hasLestip(String lestip) {
        return StringUtils.hasText(lestip);
    }

    private boolean canManageReview(Review review, String userRole, String normalizedUserSub) {
        if (isLibrarian(userRole)) {
            return true;
        }

        if (StringUtils.hasText(review.getReviewerUserSub()) && StringUtils.hasText(normalizedUserSub)) {
            return isSameUser(review.getReviewerUserSub(), normalizedUserSub);
        }

        if (review.getReviewerUserId() != null && normalizedUserSub != null) {
            Long currentUserId = resolveCurrentUserId(normalizedUserSub);
            return currentUserId != null && currentUserId.equals(review.getReviewerUserId());
        }

        return false;
    }

    private boolean isLibrarian(String userRole) {
        return LIBRARIAN_ROLE.equalsIgnoreCase(userRole);
    }

    private boolean isTeacher(String userRole) {
        return TEACHER_ROLE.equalsIgnoreCase(userRole);
    }

    private Long resolveEffectiveSchoolId(Long requestedSchoolId, String userRole, String userSub) {
        if (requestedSchoolId != null) {
            return requestedSchoolId;
        }
        if (!StringUtils.hasText(userSub)) {
            return null;
        }
        return appUserRepository.findBySub(userSub.trim())
                .filter(user -> user.getSchool() != null)
                .map(user -> user.getSchool().getId())
                .orElse(null);
    }

    private boolean isBookAccessibleToLeerling(Long bookId, String userRole, String userSub) {
        if (!STUDENT_ROLE.equalsIgnoreCase(userRole)) {
            return true;
        }
        Long schoolId = resolveEffectiveSchoolId(null, userRole, userSub);
        if (schoolId == null) {
            return true;
        }
        return repo.existsByIdAndSchool_Id(bookId, schoolId);
    }

    private void assignGoNumberIfNeeded(Book book) {
        if (book == null || StringUtils.hasText(book.getIsbn())) {
            return;
        }

        if (!StringUtils.hasText(book.getGoNumber())) {
            book.setGoNumber(generateUniqueGoNumber());
        }
    }

    private String generateUniqueGoNumber() {
        String goNumber;
        do {
            goNumber = "GO-" + String.format("%08d", GO_NUMBER_RANDOM.nextInt(100_000_000));
        } while (repo.existsByGoNumber(goNumber));

        return goNumber;
    }

    private String resolveReviewerUserName(Review review) {
        if (Boolean.TRUE.equals(review.getAnonymous())) {
            return "Anoniem";
        }

        if (review.getReviewerUserId() == null && !StringUtils.hasText(review.getReviewerUserSub())) {
            return "Anoniem";
        }

        if (review.getReviewerUserId() == null && StringUtils.hasText(review.getReviewerUserSub())) {
            try {
                return resolveDisplayNameForSub(review.getReviewerUserSub().trim());
            } catch (Exception e) {
                return review.getReviewerUserSub().trim();
            }
        }

        try {
            return appUserRepository.findById(review.getReviewerUserId())
                    .map(AppUser::getSub)
                    .map(this::resolveDisplayNameForSub)
                    .orElse(String.valueOf(review.getReviewerUserId()));
        } catch (Exception e) {
            return String.valueOf(review.getReviewerUserId());
        }
    }

    private String resolveDisplayNameForSub(String sub) {
        if (!StringUtils.hasText(sub)) {
            return "Anoniem";
        }

        try {
            return authService.getUserInfoBySub(sub.trim())
                    .onErrorResume(err -> Mono.empty())
                    .blockOptional()
                    .map(userInfo -> {
                        var fullName = userInfo.getFullName();
                        if (fullName != null && !fullName.isBlank()) {
                            return fullName;
                        }

                        var candidate = Stream.of(userInfo.getGivenName(), userInfo.getFamilyName())
                                .filter(part -> part != null && !part.isBlank())
                                .collect(Collectors.joining(" ")).trim();
                        return candidate.isEmpty() ? userInfo.getSub() : candidate;
                    })
                    .orElse(sub.trim());
        } catch (Exception e) {
            return sub.trim();
        }
    }
}