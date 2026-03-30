package com.example.demo.dto;

public class WishlistAddRequest {
    private Long bookId;

    public WishlistAddRequest() {
    }

    public WishlistAddRequest(Long bookId) {
        this.bookId = bookId;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }
}
