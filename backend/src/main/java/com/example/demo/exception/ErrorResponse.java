package com.example.demo.exception;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;

/**
 * Standaard body voor API-foutmeldingen.
 * Record: getters, toString, equals/hashCode en constructor worden automatisch gegenereerd.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        String message,
        int status,
        String error,
        String path,
        Instant timestamp,
        String code
) {
    public static ErrorResponse of(String message, int status, String error, String path, String code) {
        return new ErrorResponse(message, status, error, path, Instant.now(), code);
    }

    public static ErrorResponse of(String message, int status, String error, String path) {
        return of(message, status, error, path, null);
    }
}
