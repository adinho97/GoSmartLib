package com.example.demo.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Size;

import com.example.demo.entities.Leesniveau;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class BookDto {
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String titel;

    @NotBlank
    @Size(max = 255)
    private String auteur;

    @Size(max = 20)
    private String isbn;

    @Size(max = 11)
    private String goNumber;

    @Size(max = 10_000_000)
    private String cover;

    @Size(max = 5000)
    private String beschrijving;

    @JsonAlias("genre")
    private List<String> genres = new ArrayList<>();

    private LocalDate uitgaveDatum;

    @Min(1)
    @Max(100_000)
    private Integer paginas;

    @Size(max = 50)
    private String taal;

    @Size(max = 255)
    private String uitgeverij;

    private Leesniveau leesniveau;

    private Long schoolId;

    private String schoolNaam;

    private Integer reviewCount;

    private Double averageRating;

    private int totalCopies;

    private int availableCopies;

    private Long loanCount = 0L;

    public int getTotalCopies() {
        return totalCopies;
    }

    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }

    public int getAvailableCopies() {
        return availableCopies;
    }

    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }

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

    public String getGoNumber() {
        return goNumber;
    }

    public void setGoNumber(String goNumber) {
        this.goNumber = goNumber;
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

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = genres;
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

    public Leesniveau getLeesniveau() {
        return leesniveau;
    }

    public void setLeesniveau(Leesniveau leesniveau) {
        this.leesniveau = leesniveau;
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public String getSchoolNaam() {
        return schoolNaam;
    }

    public void setSchoolNaam(String schoolNaam) {
        this.schoolNaam = schoolNaam;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Long getLoanCount() {
        return loanCount;
    }

    public void setLoanCount(Long loanCount) {
        this.loanCount = loanCount;
    }
}
