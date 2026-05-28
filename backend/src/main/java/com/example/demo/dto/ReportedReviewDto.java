package com.example.demo.dto;

import com.example.demo.entities.ReportedReviewStatus;
import java.time.LocalDateTime;

public class ReportedReviewDto {
    private Long id;
    private BookMiniDto book;
    private ReviewMiniDto review;
    private String reporterUserSub;
    private String reporterUserName;
    private String reason;
    private LocalDateTime reportedAt;
    private ReportedReviewStatus status;

    public static class BookMiniDto {
        private Long id;
        private String titel;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitel() { return titel; }
        public void setTitel(String titel) { this.titel = titel; }
    }

    public static class ReviewMiniDto {
        private Long id;
        private Integer rating;
        private String comment;
        private String reviewerUserName;
        private boolean anonymous;
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Integer getRating() { return rating; }
        public void setRating(Integer rating) { this.rating = rating; }
        public String getComment() { return comment; }
        public void setComment(String comment) { this.comment = comment; }
        public String getReviewerUserName() { return reviewerUserName; }
        public void setReviewerUserName(String reviewerUserName) { this.reviewerUserName = reviewerUserName; }
        public boolean isAnonymous() { return anonymous; }
        public void setAnonymous(boolean anonymous) { this.anonymous = anonymous; }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BookMiniDto getBook() { return book; }
    public void setBook(BookMiniDto book) { this.book = book; }
    public ReviewMiniDto getReview() { return review; }
    public void setReview(ReviewMiniDto review) { this.review = review; }
    public String getReporterUserSub() { return reporterUserSub; }
    public void setReporterUserSub(String reporterUserSub) { this.reporterUserSub = reporterUserSub; }
    public String getReporterUserName() { return reporterUserName; }
    public void setReporterUserName(String reporterUserName) { this.reporterUserName = reporterUserName; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public LocalDateTime getReportedAt() { return reportedAt; }
    public void setReportedAt(LocalDateTime reportedAt) { this.reportedAt = reportedAt; }
    public ReportedReviewStatus getStatus() { return status; }
    public void setStatus(ReportedReviewStatus status) { this.status = status; }
}