package com.example.demo.ratelimit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Duration;

@Configuration
public class WebMvcRateLimitConfig implements WebMvcConfigurer {

    @Bean
    public InMemoryRateLimiter importIsbnRateLimiter() {
        // 10 requests per minute per client IP
        return new InMemoryRateLimiter(10, Duration.ofMinutes(1));
    }

    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor(InMemoryRateLimiter importIsbnRateLimiter) {
        return new RateLimitingInterceptor(importIsbnRateLimiter);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor(importIsbnRateLimiter()));
    }
}