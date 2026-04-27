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

    @Enumerated(EnumType.STRING)
    @Column(name = "loaned_condition")
    private BookCopy.CopyCondition loanedCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "returned_condition")
    private BookCopy.CopyCondition returnedCondition;

    @Enumerated(EnumType.STRING)
    @Column(name = "returned_status")
    private BookCopy.CopyStatus returnedStatus;

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
    public BookCopy.CopyCondition getLoanedCondition() { return loanedCondition; }
    public void setLoanedCondition(BookCopy.CopyCondition loanedCondition) { this.loanedCondition = loanedCondition; }
    public BookCopy.CopyCondition getReturnedCondition() { return returnedCondition; }
    public void setReturnedCondition(BookCopy.CopyCondition returnedCondition) { this.returnedCondition = returnedCondition; }
    public BookCopy.CopyStatus getReturnedStatus() { return returnedStatus; }
    public void setReturnedStatus(BookCopy.CopyStatus returnedStatus) { this.returnedStatus = returnedStatus; }
}