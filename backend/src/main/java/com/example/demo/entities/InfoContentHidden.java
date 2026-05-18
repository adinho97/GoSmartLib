package com.example.demo.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "info_content_hidden",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_info_content_hidden_school_item",
        columnNames = {"school_id", "info_content_id"}
    )
)
public class InfoContentHidden {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "school_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "info_content_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private InfoContent infoContent;

    public InfoContentHidden() {}

    public InfoContentHidden(School school, InfoContent infoContent) {
        this.school = school;
        this.infoContent = infoContent;
    }

    public Long getId() { return id; }
    public School getSchool() { return school; }
    public void setSchool(School school) { this.school = school; }
    public InfoContent getInfoContent() { return infoContent; }
    public void setInfoContent(InfoContent infoContent) { this.infoContent = infoContent; }
}
