package com.example.demo.dto.admin.genre;

import java.util.List;

public class GenreResponse {
    private Long id;
    private String naam;
    private List<GenreSubgenreResponse> subgenres;

    public GenreResponse() {}
    public GenreResponse(Long id, String naam, List<GenreSubgenreResponse> subgenres) {
        this.id = id;
        this.naam = naam;
        this.subgenres = subgenres;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }
    public List<GenreSubgenreResponse> getSubgenres() { return subgenres; }
    public void setSubgenres(List<GenreSubgenreResponse> subgenres) { this.subgenres = subgenres; }
}