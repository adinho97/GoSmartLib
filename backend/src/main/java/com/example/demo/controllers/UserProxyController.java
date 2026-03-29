package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/users")
public class UserProxyController {

    private final AuthService authService;

    public UserProxyController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/{sub}/profile")
    public Mono<SmartschoolUserInfo> getUserProfile(@PathVariable String sub) {
        return authService.getUserInfoBySub(sub);
    }
}