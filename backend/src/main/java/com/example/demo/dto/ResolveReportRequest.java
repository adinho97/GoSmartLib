package com.example.demo.dto;

import jakarta.validation.constraints.NotBlank;

public class ResolveReportRequest {
    @NotBlank(message = "Actie is verplicht")
    private String action;

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
}