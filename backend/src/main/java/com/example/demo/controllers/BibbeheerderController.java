package com.example.demo.controllers;

import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.services.BibbeheerderService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bibbeheerder")
public class BibbeheerderController {

    private final BibbeheerderService bibbeheerderService;

    public BibbeheerderController(BibbeheerderService bibbeheerderService) {
        this.bibbeheerderService = bibbeheerderService;
    }

    @GetMapping("/leerkrachten")
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    public List<AdminUserListItem> getLeerkrachten(Authentication authentication) {
        return bibbeheerderService.getLeerkrachtenInOwnSchool(authentication.getName());
    }

    @PatchMapping("/leerkrachten/{userId}/promote")
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    public AdminUserListItem promoteLeerkracht(@PathVariable Long userId, Authentication authentication) {
        return bibbeheerderService.promoteLeerkrachtToBibbeheerder(authentication.getName(), userId);
    }
}
