package com.example.demo.dto.admin.school;

import com.example.demo.entities.SchoolStatus;
import jakarta.validation.constraints.NotNull;

public class UpdateSchoolStatusRequest {

    @NotNull(message = "Status is verplicht")
    private SchoolStatus status;

    public SchoolStatus getStatus() {
        return status;
    }

    public void setStatus(SchoolStatus status) {
        this.status = status;
    }
}
