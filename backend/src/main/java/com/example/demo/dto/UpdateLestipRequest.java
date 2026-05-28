package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class UpdateLestipRequest {
    @Size(max = 100000000) // Support up to 100MB of character data (Base64 is ~33% larger than binary)
    private String lestip;

    public String getLestip() {
        return lestip;
    }

    public void setLestip(String lestip) {
        this.lestip = lestip;
    }
}
