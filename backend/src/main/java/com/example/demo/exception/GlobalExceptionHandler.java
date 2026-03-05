package com.example.demo.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(RateLimitExceededException.class)
        public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
                        RateLimitExceededException ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                ex.getMessage(),
                                ex.getStatus().value(),
                                ex.getStatus().getReasonPhrase(),
                                request.getRequestURI(),
                                ex.getCode());
                return ResponseEntity.status(ex.getStatus()).body(body);
        }

        @ExceptionHandler(ApiException.class)
        public ResponseEntity<ErrorResponse> handleApiException(
                        ApiException ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                ex.getMessage(),
                                ex.getStatus().value(),
                                ex.getStatus().getReasonPhrase(),
                                request.getRequestURI(),
                                ex.getCode());
                return ResponseEntity.status(ex.getStatus()).body(body);
        }

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<ErrorResponse> handleValidation(
                        MethodArgumentNotValidException ex,
                        HttpServletRequest request) {
                String message = ex.getBindingResult().getFieldErrors().stream()
                                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                                .collect(Collectors.joining(", "));
                ErrorResponse body = ErrorResponse.of(
                                message,
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                request.getRequestURI(),
                                "VALIDATION_ERROR");
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                "Er is een onverwachte fout opgetreden.",
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                request.getRequestURI(),
                                null);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
}
