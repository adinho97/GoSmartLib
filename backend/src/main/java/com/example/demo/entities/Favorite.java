package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "favorites")
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userSub;

    @Column(nullable = false)
    private Long bookId;

    public Favorite(String userSub, Long bookId) {
        this.userSub = userSub;
        this.bookId = bookId;
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public String getUserSub() {
        return userSub;
    }
    public void setUserSub(String userSub) {
        this.userSub = userSub;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    @Override
    public String toString() {
        return "Favorite{" +
                "id=" + id +
                ", userSub='" + userSub + '\'' +
                ", bookId=" + bookId +
                '}';
    }
}