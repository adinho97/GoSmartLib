package com.example.demo.dto;

import jakarta.validation.constraints.Size;

public class UpdateLestipRequest {
    @Size(max = 500)
    private String lestip;

    public String getLestip() {
        return lestip;
    }

    public void setLestip(String lestip) {
        this.lestip = lestip;
    }
}
