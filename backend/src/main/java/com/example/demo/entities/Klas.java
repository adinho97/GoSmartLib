package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "klassen", uniqueConstraints = {
        @UniqueConstraint(name = "uk_klas_school_groupid", columnNames = { "school_id", "group_id" })
})
public class Klas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Column(name = "group_id", nullable = false, length = 120)
    private String groupId;

    @Column(nullable = false, length = 255)
    private String naam;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getNaam() {
        return naam;
    }

    public void setNaam(String naam) {
        this.naam = naam;
    }
}
