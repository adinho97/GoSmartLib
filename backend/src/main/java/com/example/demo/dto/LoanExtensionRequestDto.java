package com.example.demo.dto;

import com.example.demo.entities.LoanExtensionRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class LoanExtensionRequestDto {
    private Long id;
    private Long loanId;
    private String bookTitle;
    private String bookCover;
    private String requesterSub;
    private String requesterName;
    private LocalDateTime requestDate;
    private LoanExtensionRequest.RequestStatus status;
    private LocalDate currentDueDate;
    private LocalDate newDueDate;
    private String librarianNotes;

    public LoanExtensionRequestDto() {
    }

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

    public String getBookTitle() {
        return bookTitle;
    }

    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }

    public String getBookCover() {
        return bookCover;
    }

    public void setBookCover(String bookCover) {
        this.bookCover = bookCover;
    }

    public String getRequesterSub() {
        return requesterSub;
    }

    public void setRequesterSub(String requesterSub) {
        this.requesterSub = requesterSub;
    }

    public String getRequesterName() {
        return requesterName;
    }

    public void setRequesterName(String requesterName) {
        this.requesterName = requesterName;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public LoanExtensionRequest.RequestStatus getStatus() {
        return status;
    }

    public void setStatus(LoanExtensionRequest.RequestStatus status) {
        this.status = status;
    }

    public LocalDate getCurrentDueDate() {
        return currentDueDate;
    }

    public void setCurrentDueDate(LocalDate currentDueDate) {
        this.currentDueDate = currentDueDate;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public String getLibrarianNotes() {
        return librarianNotes;
    }

    public void setLibrarianNotes(String librarianNotes) {
        this.librarianNotes = librarianNotes;
    }
}