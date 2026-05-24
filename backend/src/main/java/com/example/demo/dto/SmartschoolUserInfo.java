package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SmartschoolUserInfo {

    @JsonAlias({ "userID", "userid", "userId", "id", "userIdentifier" })
    private String sub;

    @JsonAlias({ "name", "Name" })
    private String name;

    @JsonProperty("given_name")
    @JsonAlias({ "firstname", "voornaam", "firstName" })
    private String givenName;

    @JsonProperty("family_name")
    @JsonAlias({ "surname", "naam", "achternaam", "lastName" })
    private String familyName;

    @JsonAlias({ "fullname", "fullName" })
    private String fullName;

    private String role;

    @JsonAlias({ "Basisrol", "basisrol", "type", "function" })
    private String basisrol;

    private String accessToken;

    @JsonAlias({ "platform", "Platform" })
    private String platform;

    private String refreshToken;

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGivenName() {
        return givenName;
    }

    public void setGivenName(String givenName) {
        this.givenName = givenName;
    }

    public String getFamilyName() {
        return familyName;
    }

    public void setFamilyName(String familyName) {
        this.familyName = familyName;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        if (this.role != null && !this.role.isBlank()) {
            return this.role.toLowerCase();
        }
        if (this.basisrol != null && !this.basisrol.isBlank()) {
            switch (this.basisrol.toLowerCase()) {
                case "leerling":
                    return "leerling";
                case "leerkracht":
                    return "leerkracht";
                default:
                    return this.basisrol.toLowerCase();
            }
        }
        return null;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getBasisrol() {
        return basisrol;
    }

    public void setBasisrol(String basisrol) {
        this.basisrol = basisrol;
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

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}