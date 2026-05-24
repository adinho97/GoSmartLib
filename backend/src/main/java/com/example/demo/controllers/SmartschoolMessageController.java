package com.example.demo.controllers;

import com.example.demo.config.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolMessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/smartschool")
public class SmartschoolMessageController {

    private final SmartschoolMessageService messageService;

    public SmartschoolMessageController(SmartschoolMessageService messageService) {
        this.messageService = messageService;
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/messages")
    public Mono<ResponseEntity<Void>> sendMessage(
            @RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader,
            @Valid @RequestBody SmartschoolMessageRequest request) {

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
        }

        String accessToken = authHeader.substring(7);

        return messageService.sendMessage(accessToken, request)
                .map(response -> ResponseEntity.ok().<Void>build())
                .onErrorResume(e -> Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build()));
    }
}