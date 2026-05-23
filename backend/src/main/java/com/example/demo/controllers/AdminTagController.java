package com.example.demo.controllers;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Tag;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.TagRepository;
import com.example.demo.repositories.SchoolRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/admin/tags")
@PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
public class AdminTagController {

    private final TagRepository tagRepository;
    private final SchoolRepository schoolRepository;
    private final AppUserRepository appUserRepository;

    public AdminTagController(TagRepository tagRepository, SchoolRepository schoolRepository,
            AppUserRepository appUserRepository) {
        this.tagRepository = tagRepository;
        this.schoolRepository = schoolRepository;
        this.appUserRepository = appUserRepository;
    }

    @GetMapping
    public List<Tag> getAll(Authentication authentication) {
        AppUser user = appUserRepository.findBySub(authentication.getName()).orElseThrow();
        if (user.getSchool() == null)
            return List.of();
        return tagRepository.findBySchool_Id(user.getSchool().getId());
    }

    @PostMapping
    public Tag create(@RequestBody Tag tag, Authentication authentication) {
        AppUser user = appUserRepository.findBySub(authentication.getName()).orElseThrow();
        if (user.getSchool() == null) {
            throw new IllegalStateException("Gebruiker is niet gekoppeld aan een school.");
        }
        return schoolRepository.findById(user.getSchool().getId()).map(school -> {
            tag.setSchool(school);
            return tagRepository.save(tag);
        }).orElseThrow();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        tagRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}