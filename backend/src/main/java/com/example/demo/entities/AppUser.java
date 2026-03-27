package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "app_users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String sub; // Smartschool userID, nooit naam

    @Column(nullable = false)
    private String role; // leerling, leerkracht, bibbeheerder

    @Column(nullable = false)
    private String username; // Smartschool username (bv. petersp)

    @Column
    private String smartschoolRefreshToken;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSub() { return sub; }
    public void setSub(String sub) { this.sub = sub; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getSmartschoolRefreshToken() { return smartschoolRefreshToken; }
    public void setSmartschoolRefreshToken(String smartschoolRefreshToken) { this.smartschoolRefreshToken = smartschoolRefreshToken; }
}