package com.example.demo.exception;

import org.springframework.http.HttpStatus;

public class RateLimitExceededException extends ApiException {

    public static final String CODE = "RATE_LIMIT_EXCEEDED";

    public RateLimitExceededException() {
        super("Te veel verzoeken. Wacht even voordat je opnieuw probeert.", HttpStatus.TOO_MANY_REQUESTS, CODE);
    }

    public RateLimitExceededException(String message) {
        super(message, HttpStatus.TOO_MANY_REQUESTS, CODE);
    }
}
