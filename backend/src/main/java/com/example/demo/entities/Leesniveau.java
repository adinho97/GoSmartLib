package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Leesniveau {
    A("A"),
    B("B"),
    C("C"),
    D("D");

    private final String label;

    Leesniveau(String label) {
        this.label = label;
    }

    @JsonValue
    public String getLabel() {
        return label;
    }

    @JsonCreator
    public static Leesniveau fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }

        for (Leesniveau leesniveau : values()) {
            if (leesniveau.label.equals(value.trim())) {
                return leesniveau;
            }
        }

        return null;
    }
}