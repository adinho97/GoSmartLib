package com.example.demo.config;

public class AuthLoginResponse {
    private String sub;
    private String role;
    private String username;
    private String givenName;
    private String familyName;
    private String accessToken;
    private String redirectTo;

    public AuthLoginResponse(String sub, String role, String username,
                              String givenName, String familyName) {
        this.sub = sub;
        this.role = role;
        this.username = username;
        this.givenName = givenName;
        this.familyName = familyName;
        this.redirectTo = "dashboard"; // default
    }

    public String getSub() { return sub; }
    public String getRole() { return role; }
    public String getUsername() { return username; }
    public String getGivenName() { return givenName; }
    public String getFamilyName() { return familyName; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public String getRedirectTo() { return redirectTo; }
    public void setRedirectTo(String redirectTo) { this.redirectTo = redirectTo; }
}