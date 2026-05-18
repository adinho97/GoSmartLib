package com.example.demo.oneroster.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class OneRosterEnrollment {

    private String sourcedId;
    private String status;
    private String role;
    private OneRosterUser user;
    private OneRosterClass clazz;

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

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public OneRosterUser getUser() {
        return user;
    }

    public void setUser(OneRosterUser user) {
        this.user = user;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("class")
    public OneRosterClass getClazz() {
        return clazz;
    }

    @com.fasterxml.jackson.annotation.JsonProperty("class")
    public void setClazz(OneRosterClass clazz) {
        this.clazz = clazz;
    }
}
