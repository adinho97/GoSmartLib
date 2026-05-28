package com.example.demo.dto;

public class BerichtBibRequest {
    private String librarianSub;
    private String senderSub;

    public BerichtBibRequest() {
    }

    public String getLibrarianSub() {
        return librarianSub;
    }

    public void setLibrarianSub(String librarianSub) {
        this.librarianSub = librarianSub;
    }

    public String getSenderSub() {
        return senderSub;
    }

    public void setSenderSub(String senderSub) {
        this.senderSub = senderSub;
    }
}