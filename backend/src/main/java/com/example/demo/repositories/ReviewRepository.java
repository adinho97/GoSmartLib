package com.example.demo.repositories;

import com.example.demo.entities.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByBook_IdOrderByCreatedAtDesc(Long bookId);

    long countByReviewerUserSub(String reviewerUserSub);

    long countByReviewerUserSubOrReviewerUserId(String reviewerUserSub, Long reviewerUserId);

    boolean existsByBook_IdAndReviewerUserSub(Long bookId, String reviewerUserSub);

    boolean existsByBook_IdAndReviewerUserId(Long bookId, Long reviewerUserId);

    List<Review> findByReviewerUserSubOrReviewerUserId(String reviewerUserSub, Long reviewerUserId);
}