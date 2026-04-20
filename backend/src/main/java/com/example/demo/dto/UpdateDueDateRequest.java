package com.example.demo.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public class UpdateDueDateRequest {
    @NotNull
    private LocalDate dueDate;

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }
}