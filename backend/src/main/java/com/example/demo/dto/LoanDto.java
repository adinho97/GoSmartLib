package com.example.demo.dto;

import java.time.LocalDate;

public class LoanDto {
    private Long id;
    private Long copyId;
    private Long bookId;
    private String bookTitel;
    private String bookCover;
    private String username;
    private LocalDate loanedAt;
    private LocalDate dueDate;
    private LocalDate returnedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCopyId() { return copyId; }
    public void setCopyId(Long copyId) { this.copyId = copyId; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getBookTitel() { return bookTitel; }
    public void setBookTitel(String t) { this.bookTitel = t; }
    public String getBookCover() { return bookCover; }
    public void setBookCover(String c) { this.bookCover = c; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDate getLoanedAt() { return loanedAt; }
    public void setLoanedAt(LocalDate d) { this.loanedAt = d; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate d) { this.dueDate = d; }
    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate d) { this.returnedAt = d; }
}