package com.example.demo.config;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/smartschool-login")
    public Mono<ResponseEntity<AuthLoginResponse>> smartschoolLogin(
            @RequestBody LoginRequest loginRequest) {
        return authService.processSmartschoolCallback(loginRequest.getCode())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestBody LogoutRequest logoutRequest) {
        return authService.logout(logoutRequest.getAccessToken())
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }
}