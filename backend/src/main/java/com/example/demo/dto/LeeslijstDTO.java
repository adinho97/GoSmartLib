package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LeeslijstDTO {
    private Long id;
    private String titel;
    private String description;
    private Long schoolId;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<LeeslijstBookDTO> books;
    private List<String> klasNames;

    public LeeslijstDTO() {}

    public LeeslijstDTO(Long id, String titel, String description, Long schoolId, 
                       String createdByName, LocalDateTime createdAt) {
        this.id = id;
        this.titel = titel;
        this.description = description;
        this.schoolId = schoolId;
        this.createdByName = createdByName;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<LeeslijstBookDTO> getBooks() { return books; }
    public void setBooks(List<LeeslijstBookDTO> books) { this.books = books; }

    public List<String> getKlasNames() { return klasNames; }
    public void setKlasNames(List<String> klasNames) { this.klasNames = klasNames; }

    // Inner DTO for book information
    public static class LeeslijstBookDTO {
        private Long bookId;
        private String titel;
        private String auteur;
        private String cover;
        private String genre;
        private Integer paginas;
        private String isbn;

        public LeeslijstBookDTO() {}

        public LeeslijstBookDTO(Long bookId, String titel, String auteur, String cover, 
                               String genre, Integer paginas, String isbn) {
            this.bookId = bookId;
            this.titel = titel;
            this.auteur = auteur;
            this.cover = cover;
            this.genre = genre;
            this.paginas = paginas;
            this.isbn = isbn;
        }

        // Getters and Setters
        public Long getBookId() { return bookId; }
        public void setBookId(Long bookId) { this.bookId = bookId; }

        public String getTitel() { return titel; }
        public void setTitel(String titel) { this.titel = titel; }

        public String getAuteur() { return auteur; }
        public void setAuteur(String auteur) { this.auteur = auteur; }

        public String getCover() { return cover; }
        public void setCover(String cover) { this.cover = cover; }

        public String getGenre() { return genre; }
        public void setGenre(String genre) { this.genre = genre; }

        public Integer getPaginas() { return paginas; }
        public void setPaginas(Integer paginas) { this.paginas = paginas; }

        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
    }
}
