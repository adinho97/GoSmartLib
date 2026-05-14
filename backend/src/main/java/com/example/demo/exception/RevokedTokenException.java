package com.example.demo.exception;

/**
 * Thrown when Smartschool rejects a refresh token with a permanent failure (HTTP 401).
 * This is non-retryable: the stored tokens must be cleared and the user must re-login.
 */
public class RevokedTokenException extends RuntimeException {
    public RevokedTokenException(String message) {
        super(message);
    }
}
