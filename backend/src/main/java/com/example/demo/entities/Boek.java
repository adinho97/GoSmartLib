package com.example.demo.entities;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "boeken")
public class Boek {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String titel;

    @NotBlank
    @Size(max = 255)
    private String auteur;

    @NotBlank
    @Size(max = 20)
    @Column(unique = true, nullable = false)
    private String isbn;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    @Size(max = 10_000_000)
    private String cover;

    @Column(columnDefinition = "TEXT")
    @Size(max = 5000)
    private String beschrijving;

    @Size(max = 100)
    private String genre;

    @PastOrPresent
    private LocalDate uitgaveDatum;

    @Min(1)
    @Max(100_000)
    private Integer paginas;

    @Size(max = 50)
    private String taal;

    @Size(max = 255)
    private String uitgeverij;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getAuteur() {
        return auteur;
    }

    public void setAuteur(String auteur) {
        this.auteur = auteur;
    }

    public String getIsbn() {
        return isbn;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public String getCover() {
        return cover;
    }

    public void setCover(String cover) {
        this.cover = cover;
    }

    public String getBeschrijving() {
        return beschrijving;
    }

    public void setBeschrijving(String beschrijving) {
        this.beschrijving = beschrijving;
    }

    public String getGenre() {
        return genre;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public LocalDate getUitgaveDatum() {
        return uitgaveDatum;
    }

    public void setUitgaveDatum(LocalDate uitgaveDatum) {
        this.uitgaveDatum = uitgaveDatum;
    }

    public Integer getPaginas() {
        return paginas;
    }

    public void setPaginas(Integer paginas) {
        this.paginas = paginas;
    }

    public String getTaal() {
        return taal;
    }

    public void setTaal(String taal) {
        this.taal = taal;
    }

    public String getUitgeverij() {
        return uitgeverij;
    }

    public void setUitgeverij(String uitgeverij) {
        this.uitgeverij = uitgeverij;
    }
}
