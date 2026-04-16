package com.example.demo.dto;

public class TeacherDto {

    private Long id;
    private String sub;
    private String naam;

    public TeacherDto() {
    }

    public TeacherDto(Long id, String sub, String naam) {
        this.id = id;
        this.sub = sub;
        this.naam = naam;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSub() {
        return sub;
    }

    public void setSub(String sub) {
        this.sub = sub;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }
}
