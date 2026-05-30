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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReportedReviewService {

    private final ReportedReviewRepository reportedReviewRepository;
    private final BookRepository bookRepository;
    private final ReviewRepository reviewRepository;
    private final ReviewService reviewService;
    private final AppUserRepository appUserRepository;
    private final DisplayNameResolver displayNameResolver;

    public ReportedReviewService(ReportedReviewRepository reportedReviewRepository,
                                 BookRepository bookRepository,
                                 ReviewRepository reviewRepository,
                                 ReviewService reviewService,
                                 AppUserRepository appUserRepository,
                                 DisplayNameResolver displayNameResolver) {
        this.reportedReviewRepository = reportedReviewRepository;
        this.bookRepository = bookRepository;
        this.reviewRepository = reviewRepository;
        this.reviewService = reviewService;
        this.appUserRepository = appUserRepository;
        this.displayNameResolver = displayNameResolver;
    }

    public void reportReview(Long bookId, Long reviewId, String reporterSub, String reason) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ApiException("Boek niet gevonden", HttpStatus.NOT_FOUND));
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ApiException("Review niet gevonden", HttpStatus.NOT_FOUND));

        if (reporterSub == null || reporterSub.isBlank()) {
            throw new ApiException("Gebruikersidentificatie ontbreekt.", HttpStatus.UNAUTHORIZED);
        }

        if (reportedReviewRepository.existsByReview_IdAndReporterUserSub(reviewId, reporterSub)) {
            throw new ApiException("Je hebt deze review al gerapporteerd.", HttpStatus.CONFLICT, "REVIEW_ALREADY_REPORTED");
        }

        ReportedReview report = new ReportedReview();
        report.setBook(book);
        report.setReview(review);
        report.setReporterUserSub(reporterSub);
        report.setReason(reason);
        report.setReportedAt(LocalDateTime.now());
        report.setStatus(ReportedReviewStatus.PENDING);

        reportedReviewRepository.save(report);
    }

    public List<ReportedReviewDto> getAll() {
        List<ReportedReview> reports = reportedReviewRepository.findAll().stream()
                .filter(report -> report.getStatus() == ReportedReviewStatus.PENDING)
                .toList();

        if (reports.isEmpty()) {
            return List.of();
        }

        // Collect all subs to resolve (both reporters and reviewers)
        Set<String> subsToResolve = new HashSet<>();
        for (ReportedReview r : reports) {
            subsToResolve.add(r.getReporterUserSub());
            if (!Boolean.TRUE.equals(r.getReview().getAnonymous())) {
                subsToResolve.add(r.getReview().getReviewerUserSub());
            }
        }

        // Batch resolve names for efficiency
        Map<String, String> names = displayNameResolver.resolveAll(null, subsToResolve);

        return reports.stream().map(r -> {
            ReportedReviewDto dto = toDto(r);
            dto.setReporterUserName(names.getOrDefault(r.getReporterUserSub(), r.getReporterUserSub()));
            if (!Boolean.TRUE.equals(r.getReview().getAnonymous())) {
                String sub = r.getReview().getReviewerUserSub();
                dto.getReview().setReviewerUserName(names.getOrDefault(sub, sub));
            }
            return dto;
        }).collect(Collectors.toList());
    }

    public long countPending() {
        return reportedReviewRepository.countByStatus(ReportedReviewStatus.PENDING);
    }

    public void resolveReport(Long reportId, String action) {
        ReportedReview mainReport = reportedReviewRepository.findById(reportId)
                .orElseThrow(() -> new ApiException("Melding niet gevonden", HttpStatus.NOT_FOUND));

        if ("inappropriate".equalsIgnoreCase(action)) {
            Long reviewId = mainReport.getReview().getId();
            Long bookId = mainReport.getBook().getId();

            // 1. Zoek alle meldingen die gekoppeld zijn aan deze specifieke review
            List<ReportedReview> reports = reportedReviewRepository.findByReview_Id(reviewId);

            // 2. Verwijder de meldingen eerst om de Foreign Key constraint (FK) te omzeilen
            reportedReviewRepository.deleteAll(reports);

            // 3. Nu de meldingen weg zijn, kan de review veilig verwijderd worden
            reviewService.deleteReview(bookId, reviewId, new ReviewContext(null, true, null));
        } else if ("appropriate".equalsIgnoreCase(action)) {
            Long reviewId = mainReport.getReview().getId();
            // Ook hier alle openstaande meldingen voor deze specifieke review afhandelen
            List<ReportedReview> reports = reportedReviewRepository.findByReview_Id(reviewId);
            reports.forEach(r -> r.setStatus(ReportedReviewStatus.RESOLVED_KEPT));
            reportedReviewRepository.saveAll(reports);
        } else {
            throw new ApiException("Ongeldige actie: " + action, HttpStatus.BAD_REQUEST);
        }
    }

    private ReportedReviewDto toDto(ReportedReview entity) {
        ReportedReviewDto dto = new ReportedReviewDto();
        dto.setId(entity.getId());
        dto.setReporterUserSub(entity.getReporterUserSub());
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

        if (isAnonymous) {
            reviewDto.setReviewerUserName("Anoniem");
        }

        dto.setReview(reviewDto);

        return dto;
    }
}