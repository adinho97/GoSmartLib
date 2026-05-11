package com.example.demo.dto.admin.genre;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class GenreRequest {
    @NotBlank(message = "Naam is verplicht")
    @Size(max = 100, message = "Naam is te lang")
    private String naam;

    private Long parentId;

    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }
    public Long getParentId() { return parentId; }
    public void setParentId(Long parentId) { this.parentId = parentId; }
}