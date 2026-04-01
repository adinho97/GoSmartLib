package com.example.demo.dto;

public class FavoriteAddRequest {
    private Long bookId;

    public FavoriteAddRequest() {
    }

    public FavoriteAddRequest(Long bookId) {
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}