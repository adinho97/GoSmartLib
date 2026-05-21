package com.example.demo.dto;

import java.util.List;

public class RecommendedBook {

    private Long bookId;
    private String titel;
    private String auteur;
    private List<String> genres; // Changed from singular String to List<String>
    private Double score; // 0-100: how strong the recommendation is
    private String reason;

    public RecommendedBook() {
    }

    public RecommendedBook(Long bookId, String titel, String auteur, List<String> genres, Double score, String reason) {
        this.bookId = bookId;
        this.titel = titel;
        this.auteur = auteur;
        this.genres = genres;
        this.score = score;
        this.reason = reason;
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

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
    }

    public Double getScore() {
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
