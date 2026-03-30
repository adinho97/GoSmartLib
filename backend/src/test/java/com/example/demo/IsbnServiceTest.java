package com.example.demo;

import com.example.demo.services.IsbnService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsbnServiceTest {

    private final IsbnService isbnService = new IsbnService();

    @Test
    void normalizeAndValidateIsbnShouldReturnEmptyForNullOrBlank() {
        assertFalse(isbnService.normalizeAndValidateIsbn(null).isPresent());
        assertFalse(isbnService.normalizeAndValidateIsbn("   ").isPresent());
        assertFalse(isbnService.normalizeAndValidateIsbn("---").isPresent());
    }

    @Test
    void normalizeAndValidateIsbnShouldNormalizeHyphensAndSpacesForValidIsbn13() {
        Optional<String> result = isbnService.normalizeAndValidateIsbn(" 978-0-553-80804-9 ");

        assertTrue(result.isPresent());
        assertEquals("9780553808049", result.get());
    }

    @Test
    void normalizeAndValidateIsbnShouldAcceptValidIsbn10WithLowercaseX() {
        Optional<String> result = isbnService.normalizeAndValidateIsbn("0-8044-2957-x");

        assertTrue(result.isPresent());
        assertEquals("080442957X", result.get());
    }

    @Test
    void normalizeAndValidateIsbnShouldRejectInvalidCheckDigits() {
        assertFalse(isbnService.normalizeAndValidateIsbn("9780553808040").isPresent());
        assertFalse(isbnService.normalizeAndValidateIsbn("0804429570").isPresent());
    }

    @Test
    void normalizeAndValidateIsbnShouldRejectInvalidCharactersOrLength() {
        assertFalse(isbnService.normalizeAndValidateIsbn("978055380804A").isPresent());
        assertFalse(isbnService.normalizeAndValidateIsbn("12345").isPresent());
        assertFalse(isbnService.normalizeAndValidateIsbn("ABCDEFGHIJ").isPresent());
    }

    @Test
    void requireValidNormalizedIsbnShouldReturnNormalizedValueForValidInput() {
        String normalized = isbnService.requireValidNormalizedIsbn("978-0-553-80804-9");

        assertEquals("9780553808049", normalized);
    }

    @Test
    void requireValidNormalizedIsbnShouldThrowForInvalidInput() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> isbnService.requireValidNormalizedIsbn("invalid-isbn"));

        assertEquals("Ongeldig ISBN-formaat", ex.getMessage());
    }
}
