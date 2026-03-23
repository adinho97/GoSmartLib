package com.example.demo.controllers;

import com.example.demo.dto.BookDto;
import com.example.demo.dto.CreateReviewRequest;
import com.example.demo.dto.ReviewDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.Review;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.BookService;
import com.example.demo.services.ReviewModerationService;
import com.example.demo.services.SchoolService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/boeken")
public class BookController {
    private static final Logger logger = LoggerFactory.getLogger(BookController.class);
    private static final String LIBRARIAN_ROLE = "bibbeheerder";
    private final BookRepository repo;
    private final ReviewRepository reviewRepository;
    private final BookService bookService;
    private final SchoolService schoolService;
    private final ReviewModerationService reviewModerationService;

    public BookController(BookRepository repo, ReviewRepository reviewRepository, BookService bookService,
            SchoolService schoolService, ReviewModerationService reviewModerationService) {
        this.repo = repo;
        this.reviewRepository = reviewRepository;
        this.bookService = bookService;
        this.schoolService = schoolService;
        this.reviewModerationService = reviewModerationService;
    }

    @GetMapping
    public List<BookDto> getAll(@RequestParam(required = false) Long schoolId) {
        List<Book> books = schoolId == null ? repo.findAll() : repo.findAllBySchool_Id(schoolId);

        return books
                .stream()
                .map(BookMapper::toDto)
                .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable @NonNull Long id,
            @RequestParam(required = false) Long schoolId) {
        return (schoolId == null ? repo.findById(id) : repo.findByIdAndSchool_Id(id, schoolId))
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
            entity.setSchool(school);
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
            @RequestParam(required = false) Long schoolId) {
        return bookService.findByIsbn(isbn, schoolId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/preview/{isbn}")
    public ResponseEntity<BookDto> previewByIsbn(@PathVariable @NonNull String isbn) {
        BookDto dto = bookService.fetchPreviewByIsbn(isbn);
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

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable @NonNull Long id,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @RequestParam(required = false) Long schoolId) {
        if (!isLibrarian(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        boolean exists = schoolId == null ? repo.existsById(id) : repo.existsByIdAndSchool_Id(id, schoolId);
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

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ReviewDto>> getReviews(@PathVariable @NonNull Long id) {
        if (!repo.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        List<ReviewDto> reviews = reviewRepository.findByBook_IdOrderByCreatedAtDesc(id)
                .stream()
                .map(this::toReviewDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(reviews);
    }

    @PostMapping("/{id}/reviews")
    public ResponseEntity<ReviewDto> createReview(@PathVariable @NonNull Long id,
            @Valid @RequestBody CreateReviewRequest request) {
        Book book = repo.findById(id).orElse(null);
        if (book == null) {
            return ResponseEntity.notFound().build();
        }

        Review review = new Review();
        review.setBook(book);
        review.setRating(request.getRating());
        String trimmedComment = request.getComment().trim();
        reviewModerationService.validateReviewComment(trimmedComment);
        review.setComment(trimmedComment);

        Review saved = reviewRepository.save(review);
        return ResponseEntity.status(HttpStatus.CREATED).body(toReviewDto(saved));
    }

    @DeleteMapping("/{bookId}/reviews/{reviewId}")
    public ResponseEntity<Void> deleteReview(@PathVariable @NonNull Long bookId,
            @RequestHeader(value = "X-User-Role", required = false) String userRole,
            @PathVariable @NonNull Long reviewId) {
        if (!isLibrarian(userRole)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        if (!repo.existsById(bookId)) {
            return ResponseEntity.notFound().build();
        }

        Review review = reviewRepository.findById(reviewId).orElse(null);
        if (review == null || review.getBook() == null || !bookId.equals(review.getBook().getId())) {
            return ResponseEntity.notFound().build();
        }

        reviewRepository.delete(review);
        return ResponseEntity.noContent().build();
    }

    private ReviewDto toReviewDto(Review review) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    private boolean isLibrarian(String userRole) {
        return LIBRARIAN_ROLE.equalsIgnoreCase(userRole);
    }
}