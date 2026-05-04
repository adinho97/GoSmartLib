package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserProxyController {

    private static final Logger logger = LoggerFactory.getLogger(UserProxyController.class);

    private final AuthService authService;
    private final SmartschoolMessageService smartschoolMessageService;
    private final SmartschoolProperties smartschoolProperties;

    public UserProxyController(AuthService authService,
            SmartschoolMessageService smartschoolMessageService,
            SmartschoolProperties smartschoolProperties) {
        this.authService = authService;
        this.smartschoolMessageService = smartschoolMessageService;
        this.smartschoolProperties = smartschoolProperties;
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