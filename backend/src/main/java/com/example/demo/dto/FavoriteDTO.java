package com.example.demo.dto;

public class FavoriteDTO {
    private Long id;
    private String userSub;
    private Long bookId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUserSub() { return userSub; }
    public void setUserSub(String userSub) { this.userSub = userSub; }
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
}