package com.example.demo.ratelimit;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applies rate limiting to selected endpoints, based on client IP address.
 */
public class RateLimitingInterceptor implements HandlerInterceptor {

    private final InMemoryRateLimiter limiter;

    public RateLimitingInterceptor(InMemoryRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Only limit the ISBN import endpoint and only POSTs
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Controller maps POST /api/boeken/isbn/{isbn}
        if (!"POST".equalsIgnoreCase(method) || !path.startsWith("/api/boeken/isbn/")) {
            return true; // niet van toepassing, laat door
        }

        String clientIp = resolveClientIp(request);
        boolean allowed = limiter.tryAcquire(clientIp);
        if (!allowed) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value()); // 429
            return false; // blokkeer de request
        }

        return true; // toegestaan
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}