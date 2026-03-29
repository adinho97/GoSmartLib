package com.example.demo.controllers;

import com.example.demo.repositories.AppUserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gebruikers")
public class UserController {

    private final AppUserRepository appUserRepository;

    public UserController(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @GetMapping("/leerlingen")
    public ResponseEntity<List<UserDto>> getLeerlingen() {
        List<UserDto> leerlingen = appUserRepository.findByRole("leerling")
                .stream()
                .map(u -> new UserDto(u.getSub(), u.getUsername(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(leerlingen);
    }

    public static class UserDto {
        private String sub;
        private String username;
        private String role;

        public UserDto(String sub, String username, String role) {
            this.sub = sub;
            this.username = username;
            this.role = role;
        }

        public String getSub() { return sub; }
        public String getUsername() { return username; }
        public String getRole() { return role; }
    }
}