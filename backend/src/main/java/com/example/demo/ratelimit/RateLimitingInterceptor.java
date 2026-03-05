package com.example.demo.ratelimit;

import com.example.demo.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitingInterceptor implements HandlerInterceptor {

    private final InMemoryRateLimiter limiter;

    public RateLimitingInterceptor(InMemoryRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Only limit the ISBN import endpoint and only POSTs
        String path = request.getRequestURI();
        String method = request.getMethod();

        // Controller maps POST /api/boeken/isbn/{isbn}
        if (!"POST".equalsIgnoreCase(method) || !path.startsWith("/api/boeken/isbn/")) {
            return true;
        }

        String clientIp = resolveClientIp(request);
        boolean allowed = limiter.tryAcquire(clientIp);
        if (!allowed) {
            throw new RateLimitExceededException();
        }

        return true;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}