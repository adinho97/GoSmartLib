package com.example.demo.controllers;

import com.example.demo.repositories.AppUserRepository;
import com.example.demo.entities.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/leerlingen")
    public ResponseEntity<List<UserDto>> getLeerlingen() {
        List<UserDto> leerlingen = appUserRepository.findByRole("leerling")
                .stream()
                .map(u -> new UserDto(u.getSub(), u.getRole()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(leerlingen);
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/leerlingen-met-klas")
    public ResponseEntity<List<StudentWithKlasDto>> getLeerlingenMetKlas(Authentication authentication) {
        String currentSub = authentication != null ? authentication.getName() : null;
        AppUser currentUser = currentSub == null ? null : appUserRepository.findBySub(currentSub).orElse(null);
        List<AppUser> students;

        if (currentUser != null && currentUser.getSchool() != null
                && !"SUPER_ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            students = appUserRepository.findBySchool_IdAndRole(currentUser.getSchool().getId(), "leerling");
        } else {
            students = appUserRepository.findByRole("leerling");
        }

        List<StudentWithKlasDto> leerlingen = students
                .stream()
                .map(u -> new StudentWithKlasDto(
                        u.getSub(),
                        u.getKlas() != null ? u.getKlas().getNaam() : null))
                .collect(Collectors.toList());
        return ResponseEntity.ok(leerlingen);
    }

    @GetMapping("/me/school")
    public ResponseEntity<Map<String, Object>> getCurrentUserSchool(Authentication authentication) {
        String sub = authentication != null ? authentication.getName() : null;
        AppUser user = sub == null ? null : appUserRepository.findBySub(sub).orElse(null);
        if (user == null || user.getSchool() == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        Map<String, Object> response = new HashMap<>();
        response.put("schoolId", user.getSchool().getId());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me/klas")
    public ResponseEntity<Map<String, Object>> getCurrentUserKlas(Authentication authentication) {
        String sub = authentication != null ? authentication.getName() : null;

        AppUser user = sub == null ? null : appUserRepository.findBySub(sub).orElse(null);
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

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/search")
    public ResponseEntity<List<SearchUserDto>> searchUsers(
            @RequestParam String q,
            @RequestParam Long schoolId) {
        String query = q.trim().toLowerCase();

        List<SearchUserDto> results = appUserRepository.findBySchool_Id(schoolId)
                .stream()
                .filter(u -> {
                    String username = u.getUsername() != null ? u.getUsername().toLowerCase() : "";
                    String displayName = u.getDisplayName() != null ? u.getDisplayName().toLowerCase() : "";
                    String sub = u.getSub() != null ? u.getSub().toLowerCase() : "";
                    return username.contains(query) || displayName.contains(query) || sub.contains(query);
                })
                .map(u -> new SearchUserDto(
                        u.getSub(),
                        u.getDisplayName() != null ? u.getDisplayName() : (u.getUsername() != null ? u.getUsername() : u.getSub())))
                .collect(Collectors.toList());

        return ResponseEntity.ok(results);
    }

    public static class SearchUserDto {
        private String sub;
        private String displayName;

        public SearchUserDto(String sub, String displayName) {
            this.sub = sub;
            this.displayName = displayName;
        }

        public String getSub() {
            return sub;
        }

        public String getDisplayName() {
            return displayName;
        }
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

    public static class StudentWithKlasDto {
        private String sub;
        private String klasName;

        public StudentWithKlasDto(String sub, String klasName) {
            this.sub = sub;
            this.klasName = klasName;
        }

        public String getSub() {
            return sub;
        }

        public String getKlasName() {
            return klasName;
        }
    }
}