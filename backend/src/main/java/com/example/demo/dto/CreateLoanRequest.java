package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class CreateLoanRequest {
    @NotNull  private Long bookId;
    @NotBlank private String username;
    @NotNull  private LocalDate dueDate;

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
}