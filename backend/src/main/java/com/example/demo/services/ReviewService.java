package com.example.demo.services;

import com.example.demo.dto.CreateReviewRequest;
import com.example.demo.dto.ReviewDto;
import com.example.demo.dto.SmartschoolUserInfo;
import com.example.demo.dto.UpdateReviewRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Review;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@SuppressWarnings("null")
public class ReviewService {

    private static final Logger logger = LoggerFactory.getLogger(ReviewService.class);

    private final ReviewRepository reviewRepository;
    private final BookRepository bookRepository;
    private final AppUserRepository appUserRepository;
    private final ReviewModerationService reviewModerationService;
    private final AuthService authService;

    public ReviewService(ReviewRepository reviewRepository,
            BookRepository bookRepository,
            AppUserRepository appUserRepository,
            ReviewModerationService reviewModerationService,
            AuthService authService) {
        this.reviewRepository = reviewRepository;
        this.bookRepository = bookRepository;
        this.appUserRepository = appUserRepository;
        this.reviewModerationService = reviewModerationService;
        this.authService = authService;
    }

    public List<ReviewDto> listReviews(Long bookId, ReviewContext ctx) {
        if (!bookRepository.existsById(Objects.requireNonNull(bookId, "bookId is required"))) {
            throw new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND");
        }
        requireBookAccessible(bookId, ctx);

        List<ReviewDto> reviews = reviewRepository.findByBook_IdOrderByCreatedAtDesc(bookId)
                .stream()
                .map(review -> toReviewDto(review, ctx))
                .collect(Collectors.toList());

        resolveReviewerNamesInPlace(reviews);
        return reviews;
    }

    public long countByCurrentUser(String currentUserSub) {
        if (!StringUtils.hasText(currentUserSub)) {
            throw new ApiException("Niet ingelogd", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }
        Long reviewerUserId = resolveCurrentUserId(currentUserSub);
        return reviewerUserId == null
                ? reviewRepository.countByReviewerUserSub(currentUserSub)
                : reviewRepository.countByReviewerUserSubOrReviewerUserId(currentUserSub, reviewerUserId);
    }

    public ReviewDto createReview(Long bookId, CreateReviewRequest request, ReviewContext ctx) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND"));

        requireBookAccessible(bookId, ctx);

        if (!StringUtils.hasText(ctx.userSub())) {
            throw new ApiException("Niet ingelogd", HttpStatus.UNAUTHORIZED, "UNAUTHORIZED");
        }

        boolean isAnonymous = Boolean.TRUE.equals(request.getAnonymous());
        String reviewerUserSub = ctx.userSub();
        Long reviewerUserId = isAnonymous ? null : resolveCurrentUserId(reviewerUserSub);

        if (reviewRepository.existsByBook_IdAndReviewerUserSub(bookId, reviewerUserSub)) {
            throw new ApiException("Je hebt al een review geplaatst voor dit boek",
                    HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
        }

        if (reviewerUserId != null
                && reviewRepository.existsByBook_IdAndReviewerUserId(bookId, reviewerUserId)) {
            throw new ApiException("Je hebt al een review geplaatst voor dit boek",
                    HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS");
        }

        String trimmedComment = request.getComment().trim();
        reviewModerationService.validateReviewComment(trimmedComment);

        Review review = new Review();
        review.setBook(book);
        review.setRating(request.getRating());
        review.setComment(trimmedComment);
        review.setAnonymous(isAnonymous);
        review.setReviewerUserSub(reviewerUserSub);
        review.setReviewerUserId(isAnonymous ? null : reviewerUserId);

        Review saved = reviewRepository.save(review);
        ReviewDto dto = toReviewDto(saved, ctx);
        if (!isAnonymous) {
            resolveReviewerNamesInPlace(List.of(dto));
        }
        return dto;
    }

    public ReviewDto updateReview(Long bookId, Long reviewId, UpdateReviewRequest request, ReviewContext ctx) {
        if (!StringUtils.hasText(ctx.userSub())) {
            throw new ApiException("Niet toegestaan", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!bookRepository.existsById(bookId)) {
            throw new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND");
        }

        requireBookAccessible(bookId, ctx);

        Review review = reviewRepository.findById(reviewId)
                .filter(r -> r.getBook() != null && bookId.equals(r.getBook().getId()))
                .orElseThrow(
                        () -> new ApiException("Review niet gevonden", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));

        if (!canAuthorEditReview(review, ctx.userSub())) {
            throw new ApiException("Niet toegestaan", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        String trimmedComment = request.getComment().trim();
        reviewModerationService.validateReviewComment(trimmedComment);
        review.setRating(request.getRating());
        review.setComment(trimmedComment);

        Review savedReview = reviewRepository.save(review);
        return toReviewDto(savedReview, ctx);
    }

    public void deleteReview(Long bookId, Long reviewId, ReviewContext ctx) {
        if (!ctx.isLibrarian() && !StringUtils.hasText(ctx.userSub())) {
            throw new ApiException("Niet toegestaan", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        if (!bookRepository.existsById(bookId)) {
            throw new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND, "BOOK_NOT_FOUND");
        }

        requireBookAccessible(bookId, ctx);

        Review review = reviewRepository.findById(reviewId)
                .filter(r -> r.getBook() != null && bookId.equals(r.getBook().getId()))
                .orElseThrow(
                        () -> new ApiException("Review niet gevonden", HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND"));

        if (!canManageReview(review, ctx)) {
            throw new ApiException("Niet toegestaan", HttpStatus.FORBIDDEN, "ACCESS_DENIED");
        }

        reviewRepository.delete(review);
    }

    private void requireBookAccessible(Long bookId, ReviewContext ctx) {
        Long resolvedBookId = Objects.requireNonNull(bookId, "bookId is required");
        if (ctx.leerlingSchoolId() == null) {
            return;
        }
        boolean accessible = bookRepository.existsByIdAndSchool_Id(resolvedBookId, ctx.leerlingSchoolId());
        if (!accessible) {
            throw new ApiException("Boek niet beschikbaar voor jouw school",
                    HttpStatus.FORBIDDEN, "BOOK_NOT_ACCESSIBLE");
        }
    }

    private ReviewDto toReviewDto(Review review, ReviewContext ctx) {
        ReviewDto dto = new ReviewDto();
        dto.setId(review.getId());
        dto.setRating(review.getRating());
        dto.setComment(review.getComment());
        dto.setReviewerUserId(review.getReviewerUserId());
        dto.setReviewerUserName(resolveReviewerUserName(review));
        dto.setCanManage(canManageReview(review, ctx));
        dto.setCanEdit(canAuthorEditReview(review, ctx.userSub()));
        dto.setCreatedAt(review.getCreatedAt());
        return dto;
    }

    private Long resolveCurrentUserId(String userSub) {
        if (!StringUtils.hasText(userSub)) {
            return null;
        }
        String trimmed = userSub.trim();
        try {
            return Long.parseLong(trimmed);
        } catch (NumberFormatException ignored) {
            return appUserRepository.findBySub(trimmed)
                    .map(AppUser::getId)
                    .orElse(null);
        }
    }

    private boolean canAuthorEditReview(Review review, String normalizedUserSub) {
        boolean subMatch = StringUtils.hasText(review.getReviewerUserSub())
                && StringUtils.hasText(normalizedUserSub)
                && isSameUser(review.getReviewerUserSub(), normalizedUserSub);

        boolean idMatch = false;
        if (!subMatch && review.getReviewerUserId() != null && normalizedUserSub != null) {
            Long currentUserId = resolveCurrentUserId(normalizedUserSub);
            idMatch = currentUserId != null && currentUserId.equals(review.getReviewerUserId());
        }

        return subMatch || idMatch;
    }

    private boolean canManageReview(Review review, ReviewContext ctx) {
        if (ctx.isLibrarian()) {
            return true;
        }

        String normalizedUserSub = ctx.userSub();
        boolean subMatch = false;
        if (StringUtils.hasText(review.getReviewerUserSub()) && StringUtils.hasText(normalizedUserSub)) {
            subMatch = isSameUser(review.getReviewerUserSub(), normalizedUserSub);
        }

        boolean idMatch = false;
        if (review.getReviewerUserId() != null && normalizedUserSub != null) {
            Long currentUserId = resolveCurrentUserId(normalizedUserSub);
            idMatch = currentUserId != null && currentUserId.equals(review.getReviewerUserId());
        }

        return subMatch || idMatch;
    }

    private String resolveReviewerUserName(Review review) {
        if (Boolean.TRUE.equals(review.getAnonymous())) {
            return "Anoniem";
        }
        String reviewerSub = review.getReviewerUserSub();
        if (!StringUtils.hasText(reviewerSub)) {
            if (review.getReviewerUserId() != null) {
                return String.valueOf(review.getReviewerUserId());
            }
            return "Anoniem";
        }
        String trimmedSub = reviewerSub.trim();
        boolean departed = appUserRepository.findBySub(trimmedSub)
                .map(user -> user.getDepartedAt() != null)
                .orElse(false);
        if (departed) {
            return "Oud-leerling";
        }
        return trimmedSub;
    }

    private void resolveReviewerNamesInPlace(List<ReviewDto> reviews) {
        List<ReviewDto> nonAnon = reviews.stream()
                .filter(dto -> !"Anoniem".equals(dto.getReviewerUserName())
                        && !"Oud-leerling".equals(dto.getReviewerUserName()))
                .collect(Collectors.toList());
        if (nonAnon.isEmpty()) {
            return;
        }

        try {
            Flux.fromIterable(nonAnon)
                    .flatMap(dto -> {
                        Mono<SmartschoolUserInfo> userMono = authService.getUserInfoBySub(dto.getReviewerUserName());
                        if (userMono == null) {
                            return Mono.just(dto);
                        }
                        return userMono
                                .map(info -> {
                                    dto.setReviewerUserName(formatDisplayName(info));
                                    return dto;
                                })
                                .onErrorReturn(dto);
                    })
                    .collectList()
                    .block();
        } catch (Exception ex) {
            logger.warn("Failed to resolve reviewer display names", ex);
        }
    }

    private String formatDisplayName(SmartschoolUserInfo info) {
        if (info.getFullName() != null && !info.getFullName().isBlank()) {
            return info.getFullName();
        }
        if (info.getName() != null && !info.getName().isBlank()) {
            return info.getName();
        }
        String given = info.getGivenName();
        String family = info.getFamilyName();
        if (given != null && !given.isBlank() && family != null && !family.isBlank()) {
            return given + " " + family;
        }
        if (given != null && !given.isBlank()) {
            return given;
        }
        return info.getSub() != null ? info.getSub() : "Gebruiker";
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
}
