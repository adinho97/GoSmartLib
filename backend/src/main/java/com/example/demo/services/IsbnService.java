package com.example.demo.services;

import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Optional;

@Service
public class IsbnService {

    public Optional<String> normalizeAndValidateIsbn(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }

        String normalized = rawValue
                .replace("-", "")
                .replaceAll("\\s+", "")
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            return Optional.empty();
        }

        if (normalized.length() == 10) {
            if (!normalized.matches("\\d{9}[\\dX]")) {
                return Optional.empty();
            }
            return isValidIsbn10(normalized) ? Optional.of(normalized) : Optional.empty();
        }

        if (normalized.length() == 13) {
            if (!normalized.matches("\\d{13}")) {
                return Optional.empty();
            }
            return isValidIsbn13(normalized) ? Optional.of(normalized) : Optional.empty();
        }

        return Optional.empty();
    }

    public String requireValidNormalizedIsbn(String rawValue) {
        return normalizeAndValidateIsbn(rawValue)
                .orElseThrow(() -> new IllegalArgumentException("Ongeldig ISBN-formaat"));
    }

    private boolean isValidIsbn10(String isbn10) {
        int sum = 0;
        for (int i = 0; i < 10; i++) {
            char c = isbn10.charAt(i);
            int digit = (c == 'X') ? 10 : Character.getNumericValue(c);
            sum += (10 - i) * digit;
        }
        return sum % 11 == 0;
    }

    private boolean isValidIsbn13(String isbn13) {
        int sum = 0;
        for (int i = 0; i < 12; i++) {
            int digit = Character.getNumericValue(isbn13.charAt(i));
            sum += (i % 2 == 0) ? digit : digit * 3;
        }

        int expectedCheck = (10 - (sum % 10)) % 10;
        int actualCheck = Character.getNumericValue(isbn13.charAt(12));
        return expectedCheck == actualCheck;
    }
}
