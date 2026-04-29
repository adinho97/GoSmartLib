package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "info_content")
public class InfoContent {

    public enum Sectie { STAP, FEATURE, TIP, FAQ }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sectie sectie;

    @Column(length = 500)
    private String titel;

    @Column(nullable = false, length = 2000)
    private String inhoud;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    public InfoContent() {}

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public Sectie getSectie() { return sectie; }
    public void setSectie(Sectie sectie) { this.sectie = sectie; }
    public String getTitel() { return titel; }
    public void setTitel(String titel) { this.titel = titel; }
    public String getInhoud() { return inhoud; }
    public void setInhoud(String inhoud) { this.inhoud = inhoud; }
    public int getSortOrder() { return sortOrder; }
    public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
}   