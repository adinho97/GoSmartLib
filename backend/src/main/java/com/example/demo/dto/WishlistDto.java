package com.example.demo.dto;

import java.time.LocalDateTime;

public class WishlistDto {
    private Long id;
    private Long bookId;
    private String titel;
    private String auteur;
    private String cover;
    private LocalDateTime addedAt;
    private boolean notificationEnabled;
    private LocalDateTime lastNotifiedAt;
    private int availableCopies;
    private int totalCopies;

    public WishlistDto() {
    }

    public WishlistDto(Long id, Long bookId, String titel, String auteur, String cover, LocalDateTime addedAt) {
        this.id = id;
        this.bookId = bookId;
        this.titel = titel;
        this.auteur = auteur;
        this.cover = cover;
        this.addedAt = addedAt;
        this.notificationEnabled = true;
        this.lastNotifiedAt = null;
        this.availableCopies = 0;
        this.totalCopies = 0;
    }

    public WishlistDto(Long id, Long bookId, String titel, String auteur, String cover, LocalDateTime addedAt,
            boolean notificationEnabled, LocalDateTime lastNotifiedAt) {
        this.id = id;
        this.bookId = bookId;
        this.titel = titel;
        this.auteur = auteur;
        this.cover = cover;
        this.addedAt = addedAt;
        this.notificationEnabled = notificationEnabled;
        this.lastNotifiedAt = lastNotifiedAt;
        this.availableCopies = 0;
        this.totalCopies = 0;
    }

    public WishlistDto(Long id, Long bookId, String titel, String auteur, String cover, LocalDateTime addedAt,
            boolean notificationEnabled, LocalDateTime lastNotifiedAt, int availableCopies, int totalCopies) {
        this.id = id;
        this.bookId = bookId;
        this.titel = titel;
        this.auteur = auteur;
        this.cover = cover;
        this.addedAt = addedAt;
        this.notificationEnabled = notificationEnabled;
        this.lastNotifiedAt = lastNotifiedAt;
        this.availableCopies = availableCopies;
        this.totalCopies = totalCopies;
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

    public boolean isNotificationEnabled() {
        return notificationEnabled;
    }

    public void setNotificationEnabled(boolean notificationEnabled) {
        this.notificationEnabled = notificationEnabled;
    }

    public LocalDateTime getLastNotifiedAt() {
        return lastNotifiedAt;
    }

    public void setLastNotifiedAt(LocalDateTime lastNotifiedAt) {
        this.lastNotifiedAt = lastNotifiedAt;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }
}
