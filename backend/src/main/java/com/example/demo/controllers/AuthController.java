package com.example.demo.controllers;

import com.example.demo.config.AuthLoginResponse;
import com.example.demo.config.AuthService;
import com.example.demo.config.LoginRequest;
import com.example.demo.config.LogoutRequest;
import com.example.demo.config.TokenRefreshResponse;
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
                .map(response -> {
                    response.setRedirectTo("dashboard");
                    return ResponseEntity.ok(response);
                })
                .defaultIfEmpty(ResponseEntity.status(401).build());
    }

    @PostMapping("/logout")
    public Mono<ResponseEntity<Void>> logout(@RequestBody LogoutRequest logoutRequest) {
        return authService.logout(logoutRequest.getAccessToken())
                .then(Mono.just(ResponseEntity.ok().<Void>build()));
    }

    @GetMapping("/validate-token")
    public Mono<ResponseEntity<Boolean>> validateToken(
            @RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.ok(false));
        }
        final String accessToken = authHeader.substring(7);
        return authService.validateToken(accessToken)
                .map(ResponseEntity::ok);
    }

    /**
     * Refresh access token if it's about to expire.
     * Frontend calls this proactively before token expiration to maintain session.
     */
    @PostMapping("/refresh-token")
    public Mono<ResponseEntity<TokenRefreshResponse>> refreshToken(
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        if (userSub == null || userSub.isBlank()) {
            return Mono.just(ResponseEntity.badRequest().build());
        }
        return authService.getUserInfoBySub(userSub)
                .map(userInfo -> ResponseEntity.ok(new TokenRefreshResponse(
                        userInfo.getAccessToken(),
                        "Token refreshed successfully")))
                .onErrorResume(error -> Mono.just(ResponseEntity.status(401).build()));
    }
}