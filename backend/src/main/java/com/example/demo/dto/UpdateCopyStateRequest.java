package com.example.demo.dto;

import com.example.demo.entities.BookCopy;

public class UpdateCopyStateRequest {
    private BookCopy.CopyStatus status;
    private BookCopy.CopyCondition condition;

    public BookCopy.CopyStatus getStatus() {
        return status;
    }

    public void setStatus(BookCopy.CopyStatus status) {
        this.status = status;
    }

    public BookCopy.CopyCondition getCondition() {
        return condition;
    }

    public void setCondition(BookCopy.CopyCondition condition) {
        this.condition = condition;
    }
}