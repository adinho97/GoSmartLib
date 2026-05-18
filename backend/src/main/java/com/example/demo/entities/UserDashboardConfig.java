package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "user_dashboard_config", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"userSub"})
})
public class UserDashboardConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userSub;

    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String configJson;

    public UserDashboardConfig() {
    }

    public UserDashboardConfig(String userSub, String configJson) {
        this.userSub = userSub;
        this.configJson = configJson;
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

    public String getConfigJson() {
        return configJson;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }
}
