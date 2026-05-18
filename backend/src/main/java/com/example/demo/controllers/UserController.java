package com.example.demo.controllers;

import com.example.demo.repositories.AppUserRepository;
import com.example.demo.entities.AppUser;
import com.example.demo.services.DisplayNameResolver;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/gebruikers")
public class UserController {

    private final AppUserRepository appUserRepository;
    private final DisplayNameResolver displayNameResolver;

    public UserController(AppUserRepository appUserRepository, DisplayNameResolver displayNameResolver) {
        this.appUserRepository = appUserRepository;
        this.displayNameResolver = displayNameResolver;
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

        // Warm the per-school cache via OneRoster batch (one API call, no-op if
        // already warm or no OneRoster). Lets the cheap peek() filter below
        // catch every name match, not just the ones already cached.
        displayNameResolver.warmSchoolCache(schoolId);

        List<AppUser> allUsers = appUserRepository.findBySchool_Id(schoolId);

        // Cheap in-memory filter: sub-match always counts; name-match counts when
        // the name is already in cache (free lookup). Anything not caught here is
        // either uncached or doesn't match — we skip the API call for it.
        Set<String> candidateSubs = new LinkedHashSet<>();
        for (AppUser u : allUsers) {
            String sub = u.getSub();
            if (sub == null || sub.isBlank()) {
                continue;
            }
            if (sub.toLowerCase().contains(query)) {
                candidateSubs.add(sub);
                continue;
            }
            Optional<String> cachedName = displayNameResolver.peek(sub);
            if (cachedName.isPresent() && cachedName.get().toLowerCase().contains(query)) {
                candidateSubs.add(sub);
            }
        }

        // Resolve display names only for survivors. Cache hits return instantly;
        // misses fall back to Smartschool userinfo for these subs alone.
        Map<String, String> names = displayNameResolver.resolveAll(schoolId, new ArrayList<>(candidateSubs));

        List<SearchUserDto> results = candidateSubs.stream()
                .map(sub -> new SearchUserDto(sub, names.getOrDefault(sub, sub)))
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
