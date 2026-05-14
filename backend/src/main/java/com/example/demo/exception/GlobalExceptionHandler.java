package com.example.demo.exception;

import com.example.demo.exception.RevokedTokenException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

        private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

        @ExceptionHandler(RevokedTokenException.class)
        public ResponseEntity<ErrorResponse> handleRevokedToken(
                        RevokedTokenException ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                "Sessie verlopen. Gelieve opnieuw in te loggen.",
                                HttpStatus.UNAUTHORIZED.value(),
                                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                                request.getRequestURI(),
                                "TOKEN_REVOKED");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
        }

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
                return ResponseEntity.status(ex.getStatus().value()).body(body);
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
                return ResponseEntity.status(ex.getStatus().value()).body(body);
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

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<ErrorResponse> handleIllegalArgument(
                        IllegalArgumentException ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                request.getRequestURI(),
                                "BAD_REQUEST");
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(IllegalStateException.class)
        public ResponseEntity<ErrorResponse> handleIllegalState(
                        IllegalStateException ex,
                        HttpServletRequest request) {
                ErrorResponse body = ErrorResponse.of(
                                ex.getMessage(),
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                request.getRequestURI(),
                                "BUSINESS_RULE_VIOLATION");
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(MethodArgumentTypeMismatchException.class)
        public ResponseEntity<ErrorResponse> handleTypeMismatch(
                        MethodArgumentTypeMismatchException ex,
                        HttpServletRequest request) {
                String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
                ErrorResponse body = ErrorResponse.of(
                                message,
                                HttpStatus.BAD_REQUEST.value(),
                                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                                request.getRequestURI(),
                                "BAD_REQUEST");
                return ResponseEntity.badRequest().body(body);
        }

        @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
        public ResponseEntity<ErrorResponse> handleMethodNotSupported(
                        HttpRequestMethodNotSupportedException ex,
                        HttpServletRequest request) {
                String message = ex.getMessage() != null ? ex.getMessage() : "Request method is not supported.";
                ErrorResponse body = ErrorResponse.of(
                                message,
                                HttpStatus.METHOD_NOT_ALLOWED.value(),
                                HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase(),
                                request.getRequestURI(),
                                "METHOD_NOT_ALLOWED");
                return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ErrorResponse> handleGeneric(
                        Exception ex,
                        HttpServletRequest request) {
                logger.error("Unhandled exception occurred at {}", request.getRequestURI(), ex);
                String message = ex.getMessage() != null ? ex.getMessage() : "Er is een onverwachte fout opgetreden.";
                ErrorResponse body = ErrorResponse.of(
                                message,
                                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                                request.getRequestURI(),
                                null);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
        }
}
