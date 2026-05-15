package com.example.demo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Custom handler that distinguishes between:
 * - 401 UNAUTHORIZED: User token is missing, invalid, or expired
 * (authentication failed)
 * - 403 FORBIDDEN: User is authenticated but lacks required permissions
 * (authorization failed)
 *
 * The SmartschoolAuthenticationFilter sets request attributes to indicate auth
 * failure.
 * This handler checks those attributes to return the appropriate HTTP status.
 */
@Component
public class CustomAccessDeniedHandler implements AccessDeniedHandler {

    private static final Logger logger = LoggerFactory.getLogger(CustomAccessDeniedHandler.class);

    private final ObjectMapper objectMapper;

    public CustomAccessDeniedHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // Check if authentication was attempted but failed
        Boolean authenticationAttempted = (Boolean) request.getAttribute("authenticationAttempted");
        Boolean authenticationFailed = (Boolean) request.getAttribute("authenticationFailed");
        Boolean databaseUnavailable = (Boolean) request.getAttribute("databaseUnavailable");

        int statusCode;
        String message;
        String errorCode;

        if (authenticationAttempted != null && authenticationAttempted && authenticationFailed != null
                && authenticationFailed) {
            // Authentication failed: token is missing, invalid, expired, or not found in DB
            statusCode = HttpServletResponse.SC_UNAUTHORIZED;
            message = "Uw authenticatie is ongeldig. Gelieve opnieuw in te loggen.";
            errorCode = "AUTHENTICATION_FAILED";
            logger.debug("Request denied: Authentication failed (token invalid/missing)");
        } else if (databaseUnavailable != null && databaseUnavailable) {
            // Database is unavailable
            statusCode = HttpServletResponse.SC_SERVICE_UNAVAILABLE;
            message = "Service tijdelijk niet beschikbaar. Probeer later opnieuw.";
            errorCode = "SERVICE_UNAVAILABLE";
            logger.warn("Request denied: Database unavailable");
        } else {
            // Authorization failed: user is authenticated but lacks required
            // role/permission
            statusCode = HttpServletResponse.SC_FORBIDDEN;
            message = "U bent niet gemachtigd voor deze actie.";
            errorCode = "ACCESS_DENIED";
            logger.debug("Request denied: Access denied for authenticated user");
        }

        response.setStatus(statusCode);
        response.setContentType("application/json");

        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("message", message);
        errorResponse.put("status", statusCode);
        errorResponse.put("error", HttpServletResponse.SC_UNAUTHORIZED == statusCode ? "Unauthorized"
                : HttpServletResponse.SC_SERVICE_UNAVAILABLE == statusCode ? "Service Unavailable" : "Forbidden");
        errorResponse.put("path", request.getRequestURI());
        errorResponse.put("timestamp", Instant.now());
        errorResponse.put("code", errorCode);

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
