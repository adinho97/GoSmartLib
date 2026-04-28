package com.example.demo.ratelimit;

import com.example.demo.exception.RateLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.HandlerInterceptor;

public class RateLimitingInterceptor implements HandlerInterceptor {

    private final InMemoryRateLimiter limiter;

    public RateLimitingInterceptor(InMemoryRateLimiter limiter) {
        this.limiter = limiter;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
            @NonNull Object handler) {
        // Only limit specific endpoints and only POSTs
        String path = request.getRequestURI();
        String method = request.getMethod();

        boolean isIsbnImport = "POST".equalsIgnoreCase(method) && path.startsWith("/api/boeken/isbn/");
        boolean isAdminAuth = "POST".equalsIgnoreCase(method)
                && ("/api/admin/login".equals(path) || "/api/admin/setup".equals(path));

        if (!isIsbnImport && !isAdminAuth) {
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