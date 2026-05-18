package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.entities.AppUser;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gebruikers")
public class UserController {

    private final AppUserRepository appUserRepository;
    private final AuthService authService;

    public UserController(AppUserRepository appUserRepository, AuthService authService) {
        this.appUserRepository = appUserRepository;
        this.authService = authService;
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

        List<SearchUserDto> results = Flux.fromIterable(appUserRepository.findBySchool_Id(schoolId))
                .flatMap(user -> authService.getUserInfoBySub(user.getSub())
                        .map(info -> new SearchUserDto(user.getSub(), formatDisplayName(info)))
                        .onErrorReturn(new SearchUserDto(user.getSub(), user.getSub())))
                .filter(dto -> matchesQuery(dto, query))
                .collectList()
                .block();

        return ResponseEntity.ok(results == null ? List.of() : results);
    }

    private boolean matchesQuery(SearchUserDto dto, String query) {
        String sub = dto.getSub() != null ? dto.getSub().toLowerCase() : "";
        String displayName = dto.getDisplayName() != null ? dto.getDisplayName().toLowerCase() : "";
        return sub.contains(query) || displayName.contains(query);
    }

    private String formatDisplayName(SmartschoolUserInfo info) {
        String given = info.getGivenName();
        String family = info.getFamilyName();
        if (given != null && !given.isBlank() && family != null && !family.isBlank())
            return family + " " + given;
        if (family != null && !family.isBlank())
            return family;
        if (given != null && !given.isBlank())
            return given;
        if (info.getFullName() != null && !info.getFullName().isBlank())
            return info.getFullName();
        if (info.getName() != null && !info.getName().isBlank())
            return info.getName();
        return info.getSub();
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