package com.example.demo.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.util.List;
import java.util.Map;

@Entity
public class SchoolSettings {

    @Id
    private Long schoolId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "school_id")
    private School school;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Object> messages;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private Map<String, Object> hours;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<Object> levels;

    public SchoolSettings() {
    }

    public Long getSchoolId() {
        return schoolId;
    }

    public void setSchoolId(Long schoolId) {
        this.schoolId = schoolId;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public List<Object> getMessages() {
        return messages;
    }

    public void setMessages(List<Object> messages) {
        this.messages = messages;
    }

    public Map<String, Object> getHours() {
        return hours;
    }

    public void setHours(Map<String, Object> hours) {
        this.hours = hours;
    }

    public List<Object> getLevels() {
        return levels;
    }

    public void setLevels(List<Object> levels) {
        this.levels = levels;
    }
}