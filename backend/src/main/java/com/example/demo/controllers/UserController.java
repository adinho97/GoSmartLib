package com.example.demo.controllers;

import com.example.demo.repositories.AppUserRepository;
import com.example.demo.entities.AppUser;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
                .map(u -> new UserDto(u.getSub(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(leerlingen);
    }

    @GetMapping("/me/klas")
    public ResponseEntity<Map<String, Object>> getCurrentUserKlas() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String sub = authentication.getName();

        AppUser user = appUserRepository.findBySub(sub).orElse(null);
        Map<String, Object> response = new HashMap<>();

        if (user != null && user.getKlas() != null) {
            response.put("klasId", user.getKlas().getId());
            response.put("klasName", user.getKlas().getNaam());
            response.put("schoolId", user.getKlas().getSchool().getId());
            return ResponseEntity.ok(response);
        }

        response.put("klasId", null);
        response.put("klasName", null);
        response.put("schoolId", null);
        return ResponseEntity.ok(response);
    }

    public static class UserDto {
        private String sub;
        private String role;

        public UserDto(String sub, String role) {
            this.sub = sub;
            this.role = role;
        }

        public String getSub() {
            return sub;
        }

        public String getRole() {
            return role;
        }
    }
}