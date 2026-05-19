package com.example.demo.repositories;

import com.example.demo.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBook_IdOrderByCreatedAtDesc(Long bookId);

    boolean existsByBook_IdAndReviewerUserId(Long bookId, Long reviewerUserId);

    boolean existsByBook_IdAndReviewerUserSub(Long bookId, String reviewerUserSub);

    Optional<Review> findByBook_IdAndReviewerUserId(Long bookId, Long reviewerUserId);

    long countByReviewerUserSub(String reviewerUserSub);

    long countByReviewerUserSubOrReviewerUserId(String reviewerUserSub, Long reviewerUserId);

    List<Review> findByReviewerUserSubOrReviewerUserId(String reviewerUserSub, Long reviewerUserId);
}
