package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

@Entity
@Table(name = "reported_reviews")
public class ReportedReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @NotBlank
    @Column(name = "reporter_user_sub", nullable = false)
    private String reporterUserSub;

    @NotBlank
    @Size(max = 1000)
    @Column(nullable = false, length = 1000)
    private String reason;

    @NotNull
    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportedReviewStatus status = ReportedReviewStatus.PENDING;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public Review getReview() { return review; }
    public void setReview(Review review) { this.review = review; }
    public String getReporterUserSub() { return reporterUserSub; }
    public void setReporterUserSub(String reporterUserSub) { this.reporterUserSub = reporterUserSub; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public ReportedReviewStatus getStatus() { return status; }
    public void setStatus(ReportedReviewStatus status) { this.status = status; }
}