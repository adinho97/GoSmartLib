package com.example.demo.dto;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public class ImportResultDto {
    public enum Status {
        ADDED,
        NOT_FOUND,
        INVALID_ISBN,
        ERROR
    }

    private int totalRows;
    private int uniqueIsbnsProcessed;
    private int duplicateRowsSkipped;
    private int totalCopiesAdded;
    private List<RowResult> results = new ArrayList<>();

    public int getTotalRows() {
        return totalRows;
    }

    public void setTotalRows(int totalRows) {
        this.totalRows = totalRows;
    }

    public int getUniqueIsbnsProcessed() {
        return uniqueIsbnsProcessed;
    }

    public void setUniqueIsbnsProcessed(int uniqueIsbnsProcessed) {
        this.uniqueIsbnsProcessed = uniqueIsbnsProcessed;
    }

    public int getDuplicateRowsSkipped() {
        return duplicateRowsSkipped;
    }

    public void setDuplicateRowsSkipped(int duplicateRowsSkipped) {
        this.duplicateRowsSkipped = duplicateRowsSkipped;
    }

    public int getTotalCopiesAdded() {
        return totalCopiesAdded;
    }

    public void setTotalCopiesAdded(int totalCopiesAdded) {
        this.totalCopiesAdded = totalCopiesAdded;
    }

    public List<RowResult> getResults() {
        return results;
    }

    public void setResults(List<RowResult> results) {
        this.results = results;
    }

    public Map<Status, Long> getStatusCounts() {
        Map<Status, Long> counts = new EnumMap<>(Status.class);
        for (Status status : Status.values()) {
            counts.put(status, 0L);
        }
        for (RowResult result : results) {
            counts.put(result.status(), counts.get(result.status()) + 1L);
        }
        return counts;
    }

    public record RowResult(String isbn, Status status, String message, Long bookId) {
    }
}