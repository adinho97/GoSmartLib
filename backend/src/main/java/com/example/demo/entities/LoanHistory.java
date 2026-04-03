package com.example.demo.entities;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "loan_history", indexes = {
        @Index(name = "idx_loan_history_user_sub", columnList = "userSub"),
        @Index(name = "idx_loan_history_returned_at", columnList = "returnedAt")
})
public class LoanHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long loanId;

    @Column(nullable = false)
    private Long copyId;

    @Column(nullable = false)
    private Long bookId;

    @Column(nullable = false)
    private String bookTitel;

    private String bookCover;

    @Column(nullable = false)
    private String userSub;

    @Column(nullable = false)
    private LocalDate loanedAt;

    @Column(nullable = false)
    private LocalDate dueDate;

    @Column(nullable = false)
    private LocalDate returnedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLoanId() {
        return loanId;
    }

    public void setLoanId(Long loanId) {
        this.loanId = loanId;
    }

    public Long getCopyId() {
        return copyId;
    }

    public void setCopyId(Long copyId) {
        this.copyId = copyId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getBookTitel() {
        return bookTitel;
    }

    public void setBookTitel(String bookTitel) {
        this.bookTitel = bookTitel;
    }

    public String getBookCover() {
        return bookCover;
    }

    public void setBookCover(String bookCover) {
        this.bookCover = bookCover;
    }

    public String getUserSub() {
        return userSub;
    }

    public void setUserSub(String userSub) {
        this.userSub = userSub;
    }

    public LocalDate getLoanedAt() {
        return loanedAt;
    }

    public void setLoanedAt(LocalDate loanedAt) {
        this.loanedAt = loanedAt;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public LocalDate getReturnedAt() {
        return returnedAt;
    }

    public void setReturnedAt(LocalDate returnedAt) {
        this.returnedAt = returnedAt;
    }
}
