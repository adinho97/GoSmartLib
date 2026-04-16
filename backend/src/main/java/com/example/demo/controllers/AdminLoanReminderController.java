package com.example.demo.controllers;

import com.example.demo.services.LoanReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/loan-reminders")
public class AdminLoanReminderController {

    private final LoanReminderService loanReminderService;

    public AdminLoanReminderController(LoanReminderService loanReminderService) {
        this.loanReminderService = loanReminderService;
    }

    @PostMapping("/run")
    public ResponseEntity<Void> runLoanReminders() {
        loanReminderService.sendLoanReminders();
        return ResponseEntity.ok().build();
    }
}
