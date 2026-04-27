package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = true)
    private String sub; // Smartschool userID (nullable for local super admin)

    @Column(nullable = false)
    private String role; // leerling, leerkracht, bibbeheerder, super_admin

    @Column(columnDefinition = "TEXT")
    private String smartschoolRefreshToken;

    @Column(columnDefinition = "TEXT")
    private String accessToken;

    @Column(nullable = true)
    private String platform;

    // Local auth fields
    @Column(unique = true, nullable = true)
    private String username; // For local super admin login

    @Column(columnDefinition = "TEXT")
    private String passwordHash; // bcrypt hash

    @Column(nullable = false)
    private String authType; // "smartschool" or "local"

    @Column(nullable = false)
    private Boolean isSuperAdmin; // = false by default

    public AppUser() {
        this.authType = "smartschool";
        this.isSuperAdmin = false;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getAuthType() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
    }

    public Boolean getIsSuperAdmin() {
        return isSuperAdmin;
    }

    public void setIsSuperAdmin(Boolean isSuperAdmin) {
        this.isSuperAdmin = isSuperAdmin;
    }
}