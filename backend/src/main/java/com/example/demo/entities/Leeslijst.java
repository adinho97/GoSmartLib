package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "leeslisten")
public class Leeslijst {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String titel;
    private String description;

    @ManyToOne
    @JoinColumn(name = "school_id")
    private School school;

    @ManyToOne
    @JoinColumn(name = "created_by_user_id")
    private AppUser createdBy;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @ManyToMany
    @JoinTable(
        name = "leeslijst_books",
        joinColumns = @JoinColumn(name = "leeslijst_id"),
        inverseJoinColumns = @JoinColumn(name = "book_id")
    )
    private Set<Book> books = new HashSet<>();

    @ManyToMany
    @JoinTable(
        name = "leeslijst_klassen",
        joinColumns = @JoinColumn(name = "leeslijst_id"),
        inverseJoinColumns = @JoinColumn(name = "klas_id")
    )
    private Set<Klas> klassen = new HashSet<>();

    public Leeslijst() {}

    public Leeslijst(String titel, School school, AppUser createdBy) {
        this.titel = titel;
        this.school = school;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }

    public AppUser getCreatedBy() { return createdBy; }
    public void setCreatedBy(AppUser createdBy) { this.createdBy = createdBy; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public Set<Book> getBooks() { return books; }
    public void setBooks(Set<Book> books) { this.books = books; }

    public Set<Klas> getKlassen() { return klassen; }
    public void setKlassen(Set<Klas> klassen) { this.klassen = klassen; }
}
