package com.example.demo.dto;

import com.example.demo.entities.BookCopy;
import java.time.LocalDate;

public class LoanDto {
    private Long id;
    private Long copyId;
    private Long bookId;
    private String bookTitel;
    private String bookCover;
    private String bookGenres; // Added for multiple genres
    private String userSub;
    private LocalDate loanedAt;
    private LocalDate dueDate;
    private LocalDate returnedAt;
    private BookCopy.CopyCondition loanedCondition;
    private BookCopy.CopyCondition returnedCondition;
    private BookCopy.CopyStatus returnedStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public BookCopy.CopyCondition getLoanedCondition() {
        return loanedCondition;
    }

    public void setLoanedCondition(BookCopy.CopyCondition loanedCondition) {
        this.loanedCondition = loanedCondition;
    }

    public BookCopy.CopyCondition getReturnedCondition() {
        return returnedCondition;
    }

    public void setReturnedCondition(BookCopy.CopyCondition returnedCondition) {
        this.returnedCondition = returnedCondition;
    }

    public BookCopy.CopyStatus getReturnedStatus() {
        return returnedStatus;
    }

    public void setReturnedStatus(BookCopy.CopyStatus returnedStatus) {
        this.returnedStatus = returnedStatus;
    }

    public String getBookGenres() {
        return bookGenres;
    }

    public void setBookGenres(String bookGenres) {
        this.bookGenres = bookGenres;
    }
}