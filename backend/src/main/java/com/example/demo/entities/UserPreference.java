package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "user_preferences")
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userSub;

    @Column(nullable = false)
    private String preferenceKey;

    @Column(nullable = false)
    private Boolean preferenceValue;

    public UserPreference() {
    }

    public UserPreference(String userSub, String preferenceKey, Boolean preferenceValue) {
        this.userSub = userSub;
        this.preferenceKey = preferenceKey;
        this.preferenceValue = preferenceValue;
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

    public String getPreferenceKey() {
        return preferenceKey;
    }

    public void setPreferenceKey(String preferenceKey) {
        this.preferenceKey = preferenceKey;
    }

    public Boolean getPreferenceValue() {
        return preferenceValue;
    }

    public void setPreferenceValue(Boolean preferenceValue) {
        this.preferenceValue = preferenceValue;
    }
}
