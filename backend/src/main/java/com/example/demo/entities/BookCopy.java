package com.example.demo.entities;

import jakarta.persistence.*;

@Entity
@Table(name = "book_copies")
public class BookCopy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "book_id")
    private Book book;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CopyStatus status = CopyStatus.AVAILABLE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private CopyCondition condition = CopyCondition.GOOD;

    public enum CopyStatus {
        AVAILABLE, LOANED, DAMAGED, LOST
    }

    public enum CopyCondition {
        GOOD, MODERATE, BAD
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Book getBook() { return book; }
    public void setBook(Book book) { this.book = book; }
    public CopyStatus getStatus() { return status; }
    public void setStatus(CopyStatus status) { this.status = status; }
    public CopyCondition getCondition() { return condition == null ? CopyCondition.GOOD : condition; }
    public void setCondition(CopyCondition condition) { this.condition = condition; }
}