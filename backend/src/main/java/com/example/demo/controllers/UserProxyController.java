package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserProxyController {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyController.class);

    private final AuthService authService;

    public UserProxyController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{sub}/profile")
    public Mono<SmartschoolUserInfo> getUserProfile(@PathVariable String sub) {
        logger.info("Fetching profile for user: {}", sub);
        return authService.getUserInfoBySub(sub)
                .onErrorResume(error -> {
                    logger.warn(
                            "Could not fetch full user info for sub: {}. Error: {}.",
                            sub, error.getMessage());
                    return Mono.empty();
                });
    }
}