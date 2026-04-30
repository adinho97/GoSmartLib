package com.example.demo.dto.admin.school;

import com.example.demo.entities.SchoolStatus;
import java.time.LocalDateTime;

public class CreateSchoolResponse {
    private Long id;
    private String subdomain;
    private String smartschoolUrl;
    private SchoolStatus status;
    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSubdomain() {
        return subdomain;
    }

    public void setSubdomain(String subdomain) {
        this.subdomain = subdomain;
    }

    public String getSmartschoolUrl() {
        return smartschoolUrl;
    }

    public void setSmartschoolUrl(String smartschoolUrl) {
        this.smartschoolUrl = smartschoolUrl;
    }

    public SchoolStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
