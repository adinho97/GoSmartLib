package com.example.demo.dto;

import java.util.List;

public class PagedBookResponse {
    private List<BookDto> items;
    private long total;

    public PagedBookResponse() {
    }

    public PagedBookResponse(List<BookDto> items, long total) {
        this.items = items;
        this.total = total;
    }

    public List<BookDto> getItems() {
        return items;
    }

    public void setItems(List<BookDto> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }
}