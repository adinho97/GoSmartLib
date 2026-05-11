// src/main/java/com/example/demo/dto/admin/genre/GenreResponse.java
package com.example.demo.dto.admin.genre;

public class GenreResponse {
    private Long id;
    private String naam;

    public GenreResponse() {}
    public GenreResponse(Long id, String naam) {
        this.id = id;
        this.naam = naam;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }
}