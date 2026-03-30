package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "copy_id")
    private BookCopy copy;

    @Column(nullable = false)
    private String userSub;

    @Column(nullable = false)
    private LocalDate loanedAt;

    @Column(nullable = false)
    private LocalDate dueDate;

    private LocalDate returnedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BookCopy getCopy() { return copy; }
    public void setCopy(BookCopy copy) { this.copy = copy; }
    public String getUserSub() { return userSub; }
    public void setUserSub(String userSub) { this.userSub = userSub; }
    public LocalDate getLoanedAt() { return loanedAt; }
    public void setLoanedAt(LocalDate loanedAt) { this.loanedAt = loanedAt; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDate getReturnedAt() { return returnedAt; }
    public void setReturnedAt(LocalDate returnedAt) { this.returnedAt = returnedAt; }
}