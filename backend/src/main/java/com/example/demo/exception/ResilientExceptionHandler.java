package com.example.demo.exception;

import com.example.demo.config.ConnectionPoolMonitor;
import com.example.demo.config.OAuthRefreshTokenHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.sql.SQLTransientConnectionException;
import java.time.Instant;

/**
 * Global exception handler for resilience failures.
 * 
 * Handles:
 * - Connection pool exhaustion (SQLTransientConnectionException)
 * - Database unavailability (DataAccessException)
 * - OAuth token revocation (TokenRevokedException)
 * - Auth filter failures (AccessDeniedException from DB unavailability)
 * 
 * Provides:
 * - Proper HTTP status codes
 * - Helpful error messages
 * - Connection pool metrics
 * - Request tracing for debugging
 */
@RestControllerAdvice
public class ResilientExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(ResilientExceptionHandler.class);

    private final ConnectionPoolMonitor poolMonitor;

    public ResilientExceptionHandler(ConnectionPoolMonitor poolMonitor) {
        this.poolMonitor = poolMonitor;
    }

    /**
     * Handle connection pool exhaustion.
     * 
     * Scenario: HikariPool timeout waiting for available connection.
     * Common cause: Too many requests, connection leak, or slow queries blocking
     * connections.
     */
    @ExceptionHandler(SQLTransientConnectionException.class)
    public ResponseEntity<ErrorResponse> handleConnectionPoolExhaustion(
            SQLTransientConnectionException ex,
            WebRequest request) {

        ConnectionPoolMonitor.PoolMetrics metrics = poolMonitor.getMetrics();
        logger.error("Connection pool exhausted: {}. Metrics: {}", ex.getMessage(), metrics);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database connection pool exhausted. Please retry after a moment.",
                "DB_POOL_EXHAUSTED",
                request.getDescription(false),
                metrics.toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle generic database access failures.
     * 
     * Scenario: Network down, database offline, transaction timeout.
     */
    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDataAccessException(
            DataAccessException ex,
            WebRequest request) {

        logger.error("Database access failed: {}", ex.getMessage(), ex);

        // Check if it's due to pool exhaustion
        if (ex.getCause() instanceof SQLTransientConnectionException) {
            return handleConnectionPoolExhaustion(
                    (SQLTransientConnectionException) ex.getCause(),
                    request);
        }

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Database service temporarily unavailable",
                "DATABASE_UNAVAILABLE",
                request.getDescription(false),
                poolMonitor.getMetrics().toString());

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle OAuth token revocation.
     * 
     * Scenario: Smartschool revoked user's token.
     * Frontend should redirect to login.
     */
    @ExceptionHandler(OAuthRefreshTokenHandler.TokenRevokedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRevoked(
            OAuthRefreshTokenHandler.TokenRevokedException ex,
            WebRequest request) {

        logger.warn("Token revoked: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.UNAUTHORIZED,
                "Your authentication token has been revoked. Please log in again.",
                "TOKEN_REVOKED",
                request.getDescription(false),
                null);

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    /**
     * Handle token refresh failures.
     * 
     * Scenario: OAuth service unavailable or token permanently invalid.
     */
    @ExceptionHandler(OAuthRefreshTokenHandler.TokenRefreshFailedException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshFailed(
            OAuthRefreshTokenHandler.TokenRefreshFailedException ex,
            WebRequest request) {

        logger.warn("Token refresh failed: {}", ex.getMessage());

        ErrorResponse error = new ErrorResponse(
                HttpStatus.SERVICE_UNAVAILABLE,
                "Unable to refresh authentication. Please try again later.",
                "TOKEN_REFRESH_FAILED",
                request.getDescription(false),
                null);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(error);
    }

    /**
     * Handle auth filter failures (e.g., DB unavailable during security check).
     * 
     * This typically comes from SmartschoolAuthenticationFilter or
     * JwtAuthenticationFilter
     * being unable to access the database for token validation.
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(
            AccessDeniedException ex,
            WebRequest request) {

        String message = ex.getMessage();
        boolean isDatabaseIssue = message != null && message.toLowerCase().contains("database");

        logger.warn("Access denied: {} (db_issue={})", message, isDatabaseIssue);

        HttpStatus status = isDatabaseIssue
                ? HttpStatus.SERVICE_UNAVAILABLE
                : HttpStatus.FORBIDDEN;

        String userMessage = isDatabaseIssue
                ? "System temporarily unavailable. Please try again later."
                : "Access denied";

        ErrorResponse error = new ErrorResponse(
                status,
                userMessage,
                isDatabaseIssue ? "SERVICE_UNAVAILABLE" : "ACCESS_DENIED",
                request.getDescription(false),
                isDatabaseIssue ? poolMonitor.getMetrics().toString() : null);

        return ResponseEntity.status(status).body(error);
    }

    /**
     * Catch-all for uncaught exceptions (production safety).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            WebRequest request) {

        logger.error("Uncaught exception: {}", ex.getMessage(), ex);

        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An internal server error occurred",
                "INTERNAL_ERROR",
                request.getDescription(false),
                null);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    // =========================== Response DTO ===========================

    public static class ErrorResponse {
        public final int status;
        public final String message;
        public final String code;
        public final String path;
        public final String timestamp;
        public final String debug; // Only for monitoring/logging, not for frontend

        public ErrorResponse(HttpStatus httpStatus, String message, String code, String path, String debug) {
            this.status = httpStatus.value();
            this.message = message;
            this.code = code;
            this.path = path;
            this.timestamp = Instant.now().toString();
            this.debug = debug;
        }
    }
}
