package com.example.demo.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "genres")
public class Genre {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false)
    private String naam;

    /**
     * null  = top-level genre (bv. "Non-fictie algemeen")
     * non-null = subgenre van de parent (bv. "Biografie / autobiografie")
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private Genre parent;

    @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("naam ASC")
    private List<Genre> subgenres = new ArrayList<>();

    // ── getters / setters ───────────────────────────────────────────────────

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getNaam() { return naam; }
    public void setNaam(String naam) { this.naam = naam; }

    public Genre getParent() { return parent; }
    public void setParent(Genre parent) { this.parent = parent; }

    public List<Genre> getSubgenres() { return subgenres; }
    public void setSubgenres(List<Genre> subgenres) { this.subgenres = subgenres; }
}