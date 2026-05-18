package com.example.demo.controllers;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.services.BibbeheerderService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/bibbeheerder")
public class BibbeheerderController {

    private final BibbeheerderService bibbeheerderService;
    private final AuthService authService;

    public BibbeheerderController(BibbeheerderService bibbeheerderService, AuthService authService) {
        this.bibbeheerderService = bibbeheerderService;
        this.authService = authService;
    }

    @GetMapping("/leerkrachten")
    @PreAuthorize("hasRole('BIBBEHEERDER')")
    public ResponseEntity<List<AdminUserListItem>> getLeerkrachtenInOwnSchool(
            @RequestHeader("X-User-Sub") String callerSub) {
        List<AdminUserListItem> users = bibbeheerderService.getLeerkrachtenInOwnSchool(callerSub);
        resolveDisplayNames(users);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'LEERKRACHT')") // Allowing LEERKRACHT for MijnTakenComponent
    public ResponseEntity<List<AdminUserListItem>> getAllUsersInOwnSchool(
            @RequestHeader("X-User-Sub") String callerSub) {
        List<AdminUserListItem> users = bibbeheerderService.getAllUsersInOwnSchool(callerSub);
        resolveDisplayNames(users);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/klassen")
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'LEERKRACHT')") // Allowing LEERKRACHT for MijnTakenComponent
    public ResponseEntity<List<KlasListItem>> getKlassenInOwnSchool(
            @RequestHeader("X-User-Sub") String callerSub) {
        return ResponseEntity.ok(bibbeheerderService.getKlassenInOwnSchool(callerSub));
    }

    @PatchMapping("/leerkrachten/{userId}/promote")
    @PreAuthorize("hasRole('BIBBEHEERDER')")
    public ResponseEntity<AdminUserListItem> promoteLeerkracht(
            @RequestHeader("X-User-Sub") String callerSub,
            @PathVariable Long userId) {
        // The request body is empty in the frontend, so we don't need to use it here.
        // The userId is already in the path.
        return ResponseEntity.ok(bibbeheerderService.promoteLeerkrachtToBibbeheerder(callerSub, userId));
    }

    private void resolveDisplayNames(List<AdminUserListItem> users) {
        Flux.fromIterable(users)
                .filter(u -> u.getSub() != null && !u.getSub().isBlank())
                .flatMap(u -> authService.getUserInfoBySub(u.getSub())
                        .map(info -> {
                            u.setDisplayName(formatDisplayName(info));
                            return u;
                        })
                        .onErrorReturn(u))
                .collectList()
                .block();
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
}