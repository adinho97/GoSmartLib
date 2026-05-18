package com.example.demo.dto;

import java.util.List;

public class CreateLeeslijstRequest {
    private String titel;
    private String description;
    private List<Long> bookIds;
    private List<Long> klasIds;
    private Boolean isGlobal;
    private Boolean isSchool;
    private List<String> assignedUserSubs;

    public CreateLeeslijstRequest() {}

    public CreateLeeslijstRequest(String titel, List<Long> bookIds, List<Long> klasIds) {
        this.titel = titel;
        this.bookIds = bookIds;
        this.klasIds = klasIds;
    }

    // Getters and Setters
    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<Long> getBookIds() { return bookIds; }
    public void setBookIds(List<Long> bookIds) { this.bookIds = bookIds; }

    public List<Long> getKlasIds() { return klasIds; }
    public void setKlasIds(List<Long> klasIds) { this.klasIds = klasIds; }

    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }

    public Boolean getIsSchool() { return isSchool; }
    public void setIsSchool(Boolean isSchool) { this.isSchool = isSchool; }

    public List<String> getAssignedUserSubs() { return assignedUserSubs; }
    public void setAssignedUserSubs(List<String> assignedUserSubs) { this.assignedUserSubs = assignedUserSubs; }
}
