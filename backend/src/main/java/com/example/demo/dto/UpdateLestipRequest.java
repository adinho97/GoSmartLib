package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class UpdateLestipRequest {
    @Size(max = 10000000) // Increase to ~10MB to accommodate Base64 file data
    private String lestip;

    public String getLestip() {
        return lestip;
    }

    public void setLestip(String lestip) {
        this.lestip = lestip;
    }
}
