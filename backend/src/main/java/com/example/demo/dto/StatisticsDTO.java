package com.example.demo.dto;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for aggregating library statistics.
 */
public class StatisticsDTO {
    private long totalBooks;
    private long totalLoans;
    private long activeLoans;
    private long totalUsers;
    private List<Map<String, Object>> popularBooks;
    private Map<String, Object> mostReadBook;
    private Map<String, Object> topReader;
    private Map<String, Object> topClass;
    private String school;
    private Map<String, Long> booksPerGenre;

    public StatisticsDTO() {
    }

    public long getTotalBooks() {
        return totalBooks;
    }

    public void setTotalBooks(long totalBooks) {
        this.totalBooks = totalBooks;
    }

    public long getTotalLoans() {
        return totalLoans;
    }

    public void setTotalLoans(long totalLoans) {
        this.totalLoans = totalLoans;
    }

    public long getActiveLoans() {
        return activeLoans;
    }

    public void setActiveLoans(long activeLoans) {
        this.activeLoans = activeLoans;
    }

    public long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public List<Map<String, Object>> getPopularBooks() {
        return popularBooks;
    }

    public void setPopularBooks(List<Map<String, Object>> popularBooks) {
        this.popularBooks = popularBooks;
    }

    public Map<String, Object> getMostReadBook() {
        return mostReadBook;
    }

    public void setMostReadBook(Map<String, Object> mostReadBook) {
        this.mostReadBook = mostReadBook;
    }

    public Map<String, Object> getTopReader() {
        return topReader;
    }

    public void setTopReader(Map<String, Object> topReader) {
        this.topReader = topReader;
    }

    public Map<String, Object> getTopClass() {
        return topClass;
    }

    public void setTopClass(Map<String, Object> topClass) {
        this.topClass = topClass;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public Map<String, Long> getBooksPerGenre() {
        return booksPerGenre;
    }

    public void setBooksPerGenre(Map<String, Long> booksPerGenre) {
        this.booksPerGenre = booksPerGenre;
    }
}