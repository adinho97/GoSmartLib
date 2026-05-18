package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "school_spotlight", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"schoolId", "type"})
})
public class SchoolSpotlight {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long schoolId;

    @Column(length = 10, nullable = false)
    private String type; // 'MAAND' | 'THEMA'

    private Long bookId;
    private Long setBy;

    @Column(name = "set_at")
    private LocalDateTime setAt;

    public SchoolSpotlight() {}

    public SchoolSpotlight(Long schoolId, String type, Long bookId, Long setBy) {
        this.schoolId = schoolId;
        this.type = type;
        this.bookId = bookId;
        this.setBy = setBy;
        this.setAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }

    public Long getSetBy() { return setBy; }
    public void setSetBy(Long setBy) { this.setBy = setBy; }

    public LocalDateTime getSetAt() { return setAt; }
    public void setSetAt(LocalDateTime setAt) { this.setAt = setAt; }
}
