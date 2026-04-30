package com.example.demo.config;
import jakarta.persistence.*;

@Entity
@Table(name = "class_reading_list_items", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"bookId", "schoolId"})
})
public class ClassReadingListItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long bookId;
    private Long schoolId;
    
    public ClassReadingListItem() {}

    public ClassReadingListItem(Long bookId, Long schoolId) {
        this.bookId = bookId;
        this.schoolId = schoolId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Long getBookId() { return bookId; }
    public void setBookId(Long bookId) { this.bookId = bookId; }
    
    public Long getSchoolId() { return schoolId; }
    public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
}
