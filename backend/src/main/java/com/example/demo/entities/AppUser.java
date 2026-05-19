package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String sub; // Smartschool userID

    @Column(nullable = false)
    private String role; // leerling, leerkracht, bibbeheerder

    @Column(columnDefinition = "TEXT")
    private String smartschoolRefreshToken;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = true)
    private String platform;

    @Column(nullable = false)
    private boolean active = true;

    // Set by OneRoster sync when this user no longer appears in their school's
    // enrollments. Their data is retained for a grace window, then anonymized
    // by the retention purge job. Cleared on re-appearance (within window).
    @Column(name = "departed_at", nullable = true)
    private LocalDateTime departedAt;

    // Set by the retention purge job after the row's PII has been wiped and
    // related data deleted/anonymized. Non-null means the user has been
    // through purge and should not be re-processed.
    @Column(name = "data_purged_at", nullable = true)
    private LocalDateTime dataPurgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "klas_id")
    private Klas klas;

    public AppUser() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getSmartschoolRefreshToken() {
        return smartschoolRefreshToken;
    }

    public void setSmartschoolRefreshToken(String smartschoolRefreshToken) {
        this.smartschoolRefreshToken = smartschoolRefreshToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getDepartedAt() {
        return departedAt;
    }

    public void setDepartedAt(LocalDateTime departedAt) {
        this.departedAt = departedAt;
    }

    public LocalDateTime getDataPurgedAt() {
        return dataPurgedAt;
    }

    public void setDataPurgedAt(LocalDateTime dataPurgedAt) {
        this.dataPurgedAt = dataPurgedAt;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public Klas getKlas() {
        return klas;
    }

    public void setKlas(Klas klas) {
        this.klas = klas;
    }

}