package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import com.example.demo.config.SmartschoolProperties;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserProxyController {

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
        return authService.getUserInfoBySub(sub);
    }


    // TODO: remove after testing
    @PostMapping("/{sub}/test-reminder")
    public Mono<String> sendTestReminder(@PathVariable String sub) {
        return authService.getUserInfoBySub(sub)
                .flatMap(userInfo -> {
                    SmartschoolMessageRequest request = new SmartschoolMessageRequest();
                    request.setPlatformUrl(smartschoolProperties.getApiBaseUrl());
                    request.setSubject("Test Herinnering");
                    request.setBody(
                            "Beste " + (userInfo.getGivenName() != null ? userInfo.getGivenName() : "Gebruiker") +
                                    ",\n\nDit is een handmatige test van het herinneringssysteem.");

                    return smartschoolMessageService.sendMessage(userInfo.getAccessToken(), request);
                });
    }
}