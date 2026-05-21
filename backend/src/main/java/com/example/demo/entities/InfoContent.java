package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;

@Entity
@Table(name = "info_content")
public class InfoContent {

    public enum Sectie {
        STAP, FEATURE, TIP, FAQ, CUSTOM
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id")
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Sectie sectie;

    @Column(length = 500)
    private String titel;

    @Column(nullable = false, length = 2000)
    private String inhoud;

    @Column(length = 255)
    private String customSectionTitle;

    @Column(name = "sort_order")
    private int sortOrder = 0;

    public InfoContent() {
    }

    public Long getId() {
        return id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    @JsonProperty("schoolId")
    public Long getSchoolId() {
        return school != null ? school.getId() : null;
    }

    public Sectie getSectie() {
        return sectie;
    }

    public void setSectie(Sectie sectie) {
        this.sectie = sectie;
    }

    public String getTitel() {
        return titel;
    }

    public void setTitel(String titel) {
        this.titel = titel;
    }

    public String getInhoud() {
        return inhoud;
    }

    public void setInhoud(String inhoud) {
        this.inhoud = inhoud;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public String getCustomSectionTitle() {
        return customSectionTitle;
    }

    public void setCustomSectionTitle(String customSectionTitle) {
        this.customSectionTitle = customSectionTitle;
    }
}