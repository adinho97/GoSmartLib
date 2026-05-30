package com.example.demo.repositories;

import com.example.demo.entities.ReportedReview;
import com.example.demo.entities.ReportedReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportedReviewRepository extends JpaRepository<ReportedReview, Long> {
    List<ReportedReview> findByReview_Id(Long reviewId);

    boolean existsByReview_IdAndReporterUserSub(Long reviewId, String reporterUserSub);

    long countByStatus(ReportedReviewStatus status);
}