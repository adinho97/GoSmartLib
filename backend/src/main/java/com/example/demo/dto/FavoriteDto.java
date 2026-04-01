package com.example.demo.dto;

import java.time.LocalDateTime;


public class FavoriteDto {
    private Long id;
    private Long bookId;
    private String titel;
    private String auteur;
    private String cover;
    private LocalDateTime addedAt;

    public FavoriteDto() {
    }

    public FavoriteDto(Long id, Long bookId, String titel, String auteur, String cover, LocalDateTime addedAt) {
        this.id = id;
        this.bookId = bookId;
        this.titel = titel;
        this.auteur = auteur;
        this.cover = cover;
        this.addedAt = addedAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }
}