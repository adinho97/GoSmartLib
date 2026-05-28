package com.example.demo.entities;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "loan_extension_requests")
public class LoanExtensionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_id", nullable = false)
    private Loan loan;

    @Column(name = "requester_user_sub", nullable = false)
    private String requesterUserSub;

    @Column(name = "librarian_user_sub", nullable = false)
    private String librarianUserSub;

    @Column(name = "request_date", nullable = false)
    private LocalDateTime requestDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RequestStatus status;

    @Column(name = "new_due_date")
    private LocalDate newDueDate; // Suggested new due date if approved

    @Column(name = "processed_by_librarian_sub")
    private String processedByLibrarianSub;

    @Column(name = "processed_date")
    private LocalDateTime processedDate;

    @Column(name = "librarian_notes", length = 500)
    private String librarianNotes;

    public enum RequestStatus {
        PENDING,
        APPROVED,
        REJECTED,
        CANCELLED
    }

    // Constructors
    public LoanExtensionRequest() {
        this.requestDate = LocalDateTime.now();
        this.status = RequestStatus.PENDING;
    }

    public LoanExtensionRequest(Loan loan, String requesterUserSub, String librarianUserSub) {
        this();
        this.loan = loan;
        this.requesterUserSub = requesterUserSub;
        this.librarianUserSub = librarianUserSub;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Loan getLoan() {
        return loan;
    }

    public void setLoan(Loan loan) {
        this.loan = loan;
    }

    public String getRequesterUserSub() {
        return requesterUserSub;
    }

    public void setRequesterUserSub(String requesterUserSub) {
        this.requesterUserSub = requesterUserSub;
    }

    public String getLibrarianUserSub() {
        return librarianUserSub;
    }

    public void setLibrarianUserSub(String librarianUserSub) {
        this.librarianUserSub = librarianUserSub;
    }

    public LocalDateTime getRequestDate() {
        return requestDate;
    }

    public void setRequestDate(LocalDateTime requestDate) {
        this.requestDate = requestDate;
    }

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public LocalDate getNewDueDate() {
        return newDueDate;
    }

    public void setNewDueDate(LocalDate newDueDate) {
        this.newDueDate = newDueDate;
    }

    public String getProcessedByLibrarianSub() {
        return processedByLibrarianSub;
    }

    public void setProcessedByLibrarianSub(String processedByLibrarianSub) {
        this.processedByLibrarianSub = processedByLibrarianSub;
    }

    public LocalDateTime getProcessedDate() {
        return processedDate;
    }

    public void setProcessedDate(LocalDateTime processedDate) {
        this.processedDate = processedDate;
    }

    public String getLibrarianNotes() {
        return librarianNotes;
    }

    public void setLibrarianNotes(String librarianNotes) {
        this.librarianNotes = librarianNotes;
    }
}