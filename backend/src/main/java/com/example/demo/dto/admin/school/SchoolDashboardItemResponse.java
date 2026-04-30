package com.example.demo.dto.admin.school;

import com.example.demo.entities.SchoolStatus;

public class SchoolDashboardItemResponse {
    private Long id;
    private String naam;
    private String subdomain;
    private SchoolStatus status;
    private long userCount;
    private long klasCount;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public SchoolStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }

    public long getUserCount() {
        return userCount;
    }

    public void setUserCount(long userCount) {
        this.userCount = userCount;
    }

    public long getKlasCount() {
        return klasCount;
    }

    public void setKlasCount(long klasCount) {
        this.klasCount = klasCount;
    }
}
