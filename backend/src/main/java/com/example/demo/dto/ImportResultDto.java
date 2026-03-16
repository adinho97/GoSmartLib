package com.example.demo.dto;

import java.util.ArrayList;
import java.util.List;

public class ImportResultDto {
    private List<String> successfulIsbns = new ArrayList<>();
    private List<String> failedIsbns = new ArrayList<>();

    public record FailedIsbn(String isbn, String reason, boolean isNotFound) {
    }
}