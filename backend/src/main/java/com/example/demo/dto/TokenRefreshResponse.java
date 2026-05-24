package com.example.demo.dto;

public class TokenRefreshResponse {
    private String accessToken;
    private String message;

    public TokenRefreshResponse() {
    }

    public TokenRefreshResponse(String accessToken, String message) {
        this.accessToken = accessToken;
        this.message = message;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
