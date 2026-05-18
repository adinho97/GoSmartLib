package com.example.demo.oneroster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneRosterUser {

    private String sourcedId;
    private String status;
    private String username;
    private String givenName;
    private String familyName;
    private String role;
    private OneRosterMetadata metadata;

    public String getSourcedId() {
        return sourcedId;
    }

    public void setSourcedId(String sourcedId) {
        this.sourcedId = sourcedId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public OneRosterMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(OneRosterMetadata metadata) {
        this.metadata = metadata;
    }

    public String legacyIdentifier() {
        return metadata == null ? null : metadata.getLegacyIdentifier();
    }
}
