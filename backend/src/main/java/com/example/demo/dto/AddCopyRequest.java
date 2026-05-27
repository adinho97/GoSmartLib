package com.example.demo.dto;

import com.example.demo.entities.BookCopy;

public class AddCopyRequest {
    private BookCopy.CopyCondition condition;

    public AddCopyRequest() {
        this.condition = BookCopy.CopyCondition.GOOD;
    }

    public BookCopy.CopyCondition getCondition() {
        return condition != null ? condition : BookCopy.CopyCondition.GOOD;
    }

    public void setCondition(BookCopy.CopyCondition condition) {
        this.condition = condition;
    }
}
