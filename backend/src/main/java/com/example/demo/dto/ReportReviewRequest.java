package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ReportReviewRequest {
    @NotBlank(message = "Reden voor rapportage is verplicht")
    @Size(max = 1000, message = "Reden mag maximaal 1000 tekens bevatten")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}