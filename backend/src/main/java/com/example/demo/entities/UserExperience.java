package com.example.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_experience")
public class UserExperience {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String userSub;

    @Column(nullable = false)
    private Integer totalExperience;

    @Column(columnDefinition = "TEXT")
    private String claimedBadgeRewardsJson;

    public UserExperience() {
    }

    public UserExperience(String userSub, Integer totalExperience, String claimedBadgeRewardsJson) {
        this.userSub = userSub;
        this.totalExperience = totalExperience;
        this.claimedBadgeRewardsJson = claimedBadgeRewardsJson;
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

    public Integer getTotalExperience() {
        return totalExperience;
    }

    public void setTotalExperience(Integer totalExperience) {
        this.totalExperience = totalExperience;
    }

    public String getClaimedBadgeRewardsJson() {
        return claimedBadgeRewardsJson;
    }

    public void setClaimedBadgeRewardsJson(String claimedBadgeRewardsJson) {
        this.claimedBadgeRewardsJson = claimedBadgeRewardsJson;
    }
}
