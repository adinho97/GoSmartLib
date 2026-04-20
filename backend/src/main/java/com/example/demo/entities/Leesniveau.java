package com.example.demo.entities;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum Leesniveau {
    EERSTE_TWEEDE_LEERJAAR("1ste-2de leerljaar"),
    DERDE_VIERDE_LEERJAAR("3de-4de leerjaar"),
    VIJFDE_ZESDE_LEERJAAR("5de-6de leerjaar"),
    EERSTE_GRAAD("1ste graad"),
    TWEEDE_GRAAD("2de graad"),
    DERDE_GRAAD("3de graad");

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
        if (value == null) {
            return null;
        }

        for (Leesniveau leesniveau : values()) {
            if (leesniveau.label.equals(value)) {
                return leesniveau;
            }
        }

        return null;
    }
}