package com.example.demo.dto;

import java.time.LocalDateTime;
import java.util.List;

public class LeeslijstDTO {
    private Long id;
    private String titel;
    private String description;
    private Long schoolId;
    private String createdByName;
    private String createdBySub;
    private LocalDateTime createdAt;
    private Boolean isGlobal;
    private Boolean isSchool;
    private List<LeeslijstBookDTO> books;
    private List<String> klasNames;
    private List<Long> klasIds;
    private List<UserDTO> sharedWithUsers;

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

    public String getCreatedBySub() { return createdBySub; }
    public void setCreatedBySub(String createdBySub) { this.createdBySub = createdBySub; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<LeeslijstBookDTO> getBooks() { return books; }
    public void setBooks(List<LeeslijstBookDTO> books) { this.books = books; }

    public List<String> getKlasNames() { return klasNames; }
    public void setKlasNames(List<String> klasNames) { this.klasNames = klasNames; }

    public List<Long> getKlasIds() { return klasIds; }
    public void setKlasIds(List<Long> klasIds) { this.klasIds = klasIds; }

    public Boolean getIsGlobal() { return isGlobal; }
    public void setIsGlobal(Boolean isGlobal) { this.isGlobal = isGlobal; }

    public Boolean getIsSchool() { return isSchool; }
    public void setIsSchool(Boolean isSchool) { this.isSchool = isSchool; }

    public List<UserDTO> getSharedWithUsers() { return sharedWithUsers; }
    public void setSharedWithUsers(List<UserDTO> sharedWithUsers) { this.sharedWithUsers = sharedWithUsers; }

    // Inner DTO for user information
    public static class UserDTO {
        private String sub;
        private String displayName;

        public UserDTO() {}

        public UserDTO(String sub, String displayName) {
            this.sub = sub;
            this.displayName = displayName;
        }

        public String getSub() { return sub; }
        public void setSub(String sub) { this.sub = sub; }

        public String getDisplayName() { return displayName; }
        public void setDisplayName(String displayName) { this.displayName = displayName; }
    }

    // Inner DTO for book information
    public static class LeeslijstBookDTO {
        private Long bookId;
        private String titel;
        private String auteur;
        private String cover;
        private List<String> genres;
        private Integer paginas;
        private String isbn;

        public LeeslijstBookDTO() {}

        public LeeslijstBookDTO(Long bookId, String titel, String auteur, String cover, 
                               List<String> genres, Integer paginas, String isbn) {
            this.bookId = bookId;
            this.titel = titel;
            this.auteur = auteur;
            this.cover = cover;
            this.genres = genres;
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

        public List<String> getGenres() { return genres; }
        public void setGenres(List<String> genres) { this.genres = genres; }

        public Integer getPaginas() { return paginas; }
        public void setPaginas(Integer paginas) { this.paginas = paginas; }

        public String getIsbn() { return isbn; }
        public void setIsbn(String isbn) { this.isbn = isbn; }
    }
}
