package com.example.demo.controllers;

import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.services.BibbeheerderService;
import com.example.demo.services.DisplayNameResolver;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bibbeheerder")
public class BibbeheerderController {

    private final BibbeheerderService bibbeheerderService;
    private final DisplayNameResolver displayNameResolver;

    public BibbeheerderController(BibbeheerderService bibbeheerderService,
            DisplayNameResolver displayNameResolver) {
        this.bibbeheerderService = bibbeheerderService;
        this.displayNameResolver = displayNameResolver;
    }

    @GetMapping("/leerkrachten")
    @PreAuthorize("hasRole('BIBBEHEERDER')")
    public ResponseEntity<List<AdminUserListItem>> getLeerkrachtenInOwnSchool(
            @RequestHeader("X-User-Sub") String callerSub) {
        Long schoolId = bibbeheerderService.getCallerSchoolId(callerSub);
        List<AdminUserListItem> users = bibbeheerderService.getLeerkrachtenInOwnSchool(callerSub);
        applyDisplayNames(schoolId, users);
        return ResponseEntity.ok(users);
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'LEERKRACHT')") // Allowing LEERKRACHT for MijnTakenComponent
    public ResponseEntity<List<AdminUserListItem>> getAllUsersInOwnSchool(
            @RequestHeader("X-User-Sub") String callerSub) {
        Long schoolId = bibbeheerderService.getCallerSchoolId(callerSub);
        List<AdminUserListItem> users = bibbeheerderService.getAllUsersInOwnSchool(callerSub);
        applyDisplayNames(schoolId, users);
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

    private void applyDisplayNames(Long schoolId, List<AdminUserListItem> users) {
        Map<String, String> names = displayNameResolver.resolveAll(schoolId,
                users.stream().map(AdminUserListItem::getSub).toList());
        users.forEach(u -> u.setDisplayName(names.getOrDefault(u.getSub(), u.getSub())));
    }
}
