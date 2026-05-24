package com.example.demo;

import com.example.demo.dto.CreateReviewRequest;
import com.example.demo.dto.ReviewDto;
import com.example.demo.dto.UpdateReviewRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Book;
import com.example.demo.entities.Review;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReviewRepository;
import com.example.demo.services.AuthService;
import com.example.demo.services.ReviewContext;
import com.example.demo.services.ReviewModerationService;
import com.example.demo.services.ReviewService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private BookRepository bookRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private ReviewModerationService reviewModerationService;

    @Mock
    private AuthService authService;

    @InjectMocks
    private ReviewService reviewService;

    private Book book;

    @BeforeEach
    void setUp() {
        book = new Book();
        book.setId(1L);
        book.setTitel("Dune");
    }

    private Review buildReview(Long id, String reviewerSub, Long reviewerId, boolean anonymous) {
        Review review = new Review();
        review.setId(id);
        review.setRating(4);
        review.setComment("Great");
        review.setReviewerUserSub(reviewerSub);
        review.setReviewerUserId(reviewerId);
        review.setAnonymous(anonymous);
        review.setBook(book);
        return review;
    }

    @Test
    void listReviewsShouldThrowNotFoundWhenBookMissing() {
        when(bookRepository.existsById(1L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.listReviews(1L, new ReviewContext("sub", false, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void listReviewsShouldThrowForbiddenWhenStudentSchoolMismatch() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(bookRepository.existsByIdAndSchool_Id(1L, 100L)).thenReturn(false);

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.listReviews(1L, new ReviewContext("sub", false, 100L)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void listReviewsShouldReturnMappedDtos() {
        Review own = buildReview(10L, "alice", null, false);
        Review other = buildReview(11L, "bob", null, false);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByBook_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(own, other));
        when(appUserRepository.findBySub(any())).thenReturn(Optional.empty());
        when(authService.getUserInfoBySub(any())).thenReturn(null);

        List<ReviewDto> dtos = reviewService.listReviews(1L,
                new ReviewContext("alice", false, null));

        assertEquals(2, dtos.size());
        ReviewDto ownDto = dtos.stream().filter(d -> d.getId() == 10L).findFirst().orElseThrow();
        ReviewDto otherDto = dtos.stream().filter(d -> d.getId() == 11L).findFirst().orElseThrow();
        assertTrue(ownDto.isCanEdit(), "author can edit own review");
        assertTrue(ownDto.isCanManage(), "author can manage own review");
        assertFalse(otherDto.isCanEdit(), "non-author cannot edit");
        assertFalse(otherDto.isCanManage(), "non-author non-librarian cannot manage");
    }

    @Test
    void listReviewsShouldExposeCanManageForLibrarianButNotCanEdit() {
        Review other = buildReview(11L, "bob", null, false);
        when(bookRepository.existsById(1L)).thenReturn(true);
        when(reviewRepository.findByBook_IdOrderByCreatedAtDesc(1L)).thenReturn(List.of(other));
        when(appUserRepository.findBySub(any())).thenReturn(Optional.empty());
        when(authService.getUserInfoBySub(any())).thenReturn(null);

        List<ReviewDto> dtos = reviewService.listReviews(1L,
                new ReviewContext("librarian-sub", true, null));

        assertTrue(dtos.get(0).isCanManage(), "librarian can manage any review");
        assertFalse(dtos.get(0).isCanEdit(), "librarian cannot edit other's review");
    }

    @Test
    void countByCurrentUserShouldThrowUnauthorizedWhenNoSub() {
        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.countByCurrentUser(null));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void countByCurrentUserShouldQueryBothSubAndIdWhenUserResolves() {
        AppUser user = new AppUser();
        user.setId(42L);
        when(appUserRepository.findBySub("alice")).thenReturn(Optional.of(user));
        when(reviewRepository.countByReviewerUserSubOrReviewerUserId("alice", 42L)).thenReturn(7L);

        assertEquals(7L, reviewService.countByCurrentUser("alice"));
    }

    @Test
    void createReviewShouldThrowNotFoundWhenBookMissing() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("good");
        when(bookRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.createReview(99L, req, new ReviewContext("alice", false, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void createReviewShouldThrowConflictWhenUserAlreadyReviewed() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("good");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(reviewRepository.existsByBook_IdAndReviewerUserSub(1L, "alice")).thenReturn(true);

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.createReview(1L, req, new ReviewContext("alice", false, null)));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    void createReviewShouldThrowUnauthorizedWhenSubMissing() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment("good");
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.createReview(1L, req, new ReviewContext(null, false, null)));
        assertEquals(HttpStatus.UNAUTHORIZED, ex.getStatus());
    }

    @Test
    void createReviewShouldOmitReviewerUserIdWhenAnonymous() {
        CreateReviewRequest req = new CreateReviewRequest();
        req.setRating(5);
        req.setComment(" trimmed ");
        req.setAnonymous(true);
        when(bookRepository.findById(1L)).thenReturn(Optional.of(book));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> {
            Review r = inv.getArgument(0);
            r.setId(50L);
            return r;
        });

        ReviewDto dto = reviewService.createReview(1L, req, new ReviewContext("alice", false, null));

        assertEquals(50L, dto.getId());
        assertNull(dto.getReviewerUserId());
        verify(reviewModerationService).validateReviewComment("trimmed");
    }

    @Test
    void updateReviewShouldThrowForbiddenWhenNotAuthor() {
        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(5);
        req.setComment("updated");
        when(bookRepository.existsById(1L)).thenReturn(true);
        Review existing = buildReview(10L, "bob", null, false);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.updateReview(1L, 10L, req,
                        new ReviewContext("alice", false, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void updateReviewShouldSucceedWhenAuthor() {
        UpdateReviewRequest req = new UpdateReviewRequest();
        req.setRating(5);
        req.setComment(" new comment ");
        when(bookRepository.existsById(1L)).thenReturn(true);
        Review existing = buildReview(10L, "alice", null, false);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);
        when(appUserRepository.findBySub(any())).thenReturn(Optional.empty());

        ReviewDto dto = reviewService.updateReview(1L, 10L, req,
                new ReviewContext("alice", false, null));

        assertEquals(10L, dto.getId());
        verify(reviewModerationService).validateReviewComment("new comment");
        verify(reviewRepository).save(existing);
    }

    @Test
    void deleteReviewShouldAllowLibrarianForOthersReview() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        Review existing = buildReview(10L, "bob", null, false);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        reviewService.deleteReview(1L, 10L, new ReviewContext("librarian", true, null));

        verify(reviewRepository).delete(existing);
    }

    @Test
    void deleteReviewShouldRejectNonLibrarianNonAuthor() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        Review existing = buildReview(10L, "bob", null, false);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.deleteReview(1L, 10L,
                        new ReviewContext("alice", false, null)));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        verify(reviewRepository, never()).delete(any());
    }

    @Test
    void deleteReviewShouldReturnNotFoundWhenReviewBelongsToDifferentBook() {
        when(bookRepository.existsById(1L)).thenReturn(true);
        Book otherBook = new Book();
        otherBook.setId(2L);
        Review existing = buildReview(10L, "bob", null, false);
        existing.setBook(otherBook);
        when(reviewRepository.findById(10L)).thenReturn(Optional.of(existing));

        ApiException ex = assertThrows(ApiException.class,
                () -> reviewService.deleteReview(1L, 10L, new ReviewContext("librarian", true, null)));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }
}
