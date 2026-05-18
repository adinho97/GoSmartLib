package com.example.demo.oneroster.dto;

import java.util.ArrayList;
import java.util.List;

public class OneRosterSyncResult {

    private String subdomain;
    private int schoolsCreated;
    private int schoolsUpdated;
    private int classesCreated;
    private int classesUpdated;
    private int usersCreated;
    private int usersUpdated;
    private int skipped;
    private boolean skippedEntirely;
    private String skipReason;
    private List<String> errors = new ArrayList<>();

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public int getSchoolsCreated() {
        return schoolsCreated;
    }

    public void incrementSchoolsCreated() {
        this.schoolsCreated++;
    }

    public int getSchoolsUpdated() {
        return schoolsUpdated;
    }

    public void incrementSchoolsUpdated() {
        this.schoolsUpdated++;
    }

    public int getClassesCreated() {
        return classesCreated;
    }

    public void incrementClassesCreated() {
        this.classesCreated++;
    }

    public int getClassesUpdated() {
        return classesUpdated;
    }

    public void incrementClassesUpdated() {
        this.classesUpdated++;
    }

    public int getUsersCreated() {
        return usersCreated;
    }

    public void incrementUsersCreated() {
        this.usersCreated++;
    }

    public int getUsersUpdated() {
        return usersUpdated;
    }

    public void incrementUsersUpdated() {
        this.usersUpdated++;
    }

    public int getSkipped() {
        return skipped;
    }

    public void incrementSkipped() {
        this.skipped++;
    }

    public boolean isSkippedEntirely() {
        return skippedEntirely;
    }

    public void setSkippedEntirely(boolean skippedEntirely) {
        this.skippedEntirely = skippedEntirely;
    }

    public String getSkipReason() {
        return skipReason;
    }

    public void setSkipReason(String skipReason) {
        this.skipReason = skipReason;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void addError(String error) {
        this.errors.add(error);
    }
}
