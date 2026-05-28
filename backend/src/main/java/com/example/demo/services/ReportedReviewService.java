package com.example.demo.services;

import com.example.demo.dto.ReportedReviewDto;
import com.example.demo.entities.*;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.ReportedReviewRepository;
import com.example.demo.repositories.ReviewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportedReviewService {

    private final ReportedReviewRepository reportedReviewRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final AppUserRepository appUserRepository;

    public ReportedReviewService(ReportedReviewRepository reportedReviewRepository,
                                 BookRepository bookRepository,
                                 ReviewRepository reviewRepository,
                                 ReviewService reviewService,
                                 AppUserRepository appUserRepository) {
        this.reportedReviewRepository = reportedReviewRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.appUserRepository = appUserRepository;
    }

    public void reportReview(Long bookId, Long reviewId, String reporterSub, String reporterName, String reason) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review niet gevonden", HttpStatus.NOT_FOUND));

        if (reportedReviewRepository.existsByReviewIdAndReporterUserSub(reviewId, reporterSub)) {
            throw new ApiException("Je hebt deze review al gerapporteerd.", HttpStatus.CONFLICT, "REVIEW_ALREADY_REPORTED");
        }

        ReportedReview report = new ReportedReview();
        report.setBook(book);
        report.setReview(review);
        report.setReporterUserSub(reporterSub);
        report.setReporterUserName(reporterName);
        report.setReason(reason);
        report.setReportedAt(LocalDateTime.now());
        report.setStatus(ReportedReviewStatus.PENDING);

        reportedReviewRepository.save(report);
    }

    public List<ReportedReviewDto> getAll() {
        return reportedReviewRepository.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public long countPending() {
        return reportedReviewRepository.countByStatus(ReportedReviewStatus.PENDING);
    }

    public void resolveReport(Long reportId, String action) {
        ReportedReview mainReport = reportedReviewRepository.findById(reportId)
                .orElseThrow(() -> new ApiException("Melding niet gevonden", HttpStatus.NOT_FOUND));

        if ("inappropriate".equalsIgnoreCase(action)) {
            Long reviewId = mainReport.getReview().getId();
            reviewService.deleteReview(mainReport.getBook().getId(), reviewId, new ReviewContext(null, true, null));
            
            List<ReportedReview> reports = reportedReviewRepository.findByReviewId(reviewId);
            reports.forEach(r -> r.setStatus(ReportedReviewStatus.RESOLVED_DELETED));
            reportedReviewRepository.saveAll(reports);
        } else if ("appropriate".equalsIgnoreCase(action)) {
            mainReport.setStatus(ReportedReviewStatus.RESOLVED_KEPT);
            reportedReviewRepository.save(mainReport);
        } else {
            throw new ApiException("Ongeldige actie: " + action, HttpStatus.BAD_REQUEST);
        }
    }

    private ReportedReviewDto toDto(ReportedReview entity) {
        ReportedReviewDto dto = new ReportedReviewDto();
        dto.setId(entity.getId());
        dto.setReporterUserSub(entity.getReporterUserSub());
        dto.setReporterUserName(entity.getReporterUserName());
        dto.setReason(entity.getReason());
        dto.setReportedAt(entity.getReportedAt());
        dto.setStatus(entity.getStatus());

        ReportedReviewDto.BookMiniDto bookDto = new ReportedReviewDto.BookMiniDto();
        bookDto.setId(entity.getBook().getId());
        bookDto.setTitel(entity.getBook().getTitel());
        dto.setBook(bookDto);

        ReportedReviewDto.ReviewMiniDto reviewDto = new ReportedReviewDto.ReviewMiniDto();
        reviewDto.setId(entity.getReview().getId());
        reviewDto.setRating(entity.getReview().getRating());
        reviewDto.setComment(entity.getReview().getComment());

        // Resolving anonymity and name
        boolean isAnonymous = Boolean.TRUE.equals(entity.getReview().getAnonymous());
        reviewDto.setAnonymous(isAnonymous);

        String reviewerName = isAnonymous ? "Anoniem" : entity.getReview().getReviewerUserSub();
        reviewDto.setReviewerUserName(reviewerName);

        dto.setReview(reviewDto);

        return dto;
    }
}