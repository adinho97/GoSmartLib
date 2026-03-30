package com.example.demo.dto;

import com.example.demo.entities.BookCopy;

public class CopyDto {
    private Long id;
    private BookCopy.CopyStatus status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BookCopy.CopyStatus getStatus() { return status; }
    public void setStatus(BookCopy.CopyStatus status) { this.status = status; }
}