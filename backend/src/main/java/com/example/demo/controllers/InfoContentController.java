package com.example.demo.controllers;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.entities.InfoContentHidden;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InfoContentHiddenRepository;
import com.example.demo.repositories.InfoContentRepository;
import com.example.demo.services.SchoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/info-content")
@SuppressWarnings("null")
public class InfoContentController {

    private static final String ROLE_SUPER_ADMIN = "ROLE_SUPER_ADMIN";

    private final InfoContentRepository infoContentRepository;
    private final InfoContentHiddenRepository infoContentHiddenRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolService schoolService;

    public InfoContentController(InfoContentRepository infoContentRepository,
                                 InfoContentHiddenRepository infoContentHiddenRepository,
                                 AppUserRepository appUserRepository,
                                 SchoolService schoolService) {
        this.infoContentRepository = infoContentRepository;
        this.infoContentHiddenRepository = infoContentHiddenRepository;
        this.appUserRepository = appUserRepository;
        this.schoolService = schoolService;
    }

    // Authenticated read.
    // - super_admin: schoolId param is honoured (null = only globals).
    // - everyone else: schoolId param is ignored; resolved from authenticated user's school.
    @GetMapping
    public List<InfoContent> getAll(
            @RequestParam(required = false) Long schoolId,
            @RequestParam Sectie sectie,
            Authentication authentication) {
        Long resolvedSchoolId;
        if (isSuperAdmin(authentication)) {
            if (schoolId == null) {
                return infoContentRepository.findGlobals(sectie);
            }
            resolvedSchoolId = schoolService.getByIdOrDefault(schoolId).getId();
        } else {
            resolvedSchoolId = resolveCallerSchool(authentication).getId();
        }
        return infoContentRepository.findVisibleForSchool(resolvedSchoolId, sectie);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody InfoContentRequest request,
                                    Authentication authentication) {
        if (request.getSectie() == null) {
            return ResponseEntity.badRequest().body("sectie is required");
        }
        if (request.getInhoud() == null || request.getInhoud().isBlank()) {
            return ResponseEntity.badRequest().body("inhoud is required");
        }

        boolean superAdmin = isSuperAdmin(authentication);
        Sectie sectie = request.getSectie();

        InfoContent item = new InfoContent();
        item.setSectie(sectie);
        item.setTitel(request.getTitel());
        item.setInhoud(request.getInhoud());
        item.setSortOrder(request.getSortOrder());

        if (isGlobalOnlySection(sectie)) {
            // STAP and FEATURE are always global; only super_admin may create them.
            if (!superAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            item.setSchool(null);
        } else {
            // TIP and FAQ may be global (super_admin) or per-school.
            if (superAdmin) {
                if (request.getSchoolId() != null) {
                    item.setSchool(schoolService.getByIdOrDefault(request.getSchoolId()));
                } else {
                    item.setSchool(null);
                }
            } else {
                // bibbeheerder: always force to their own school, ignore request body.
                item.setSchool(resolveCallerSchool(authentication));
            }
        }
        return ResponseEntity.ok(infoContentRepository.save(item));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                    @RequestBody InfoContentRequest request,
                                    Authentication authentication) {
        return infoContentRepository.findById(id).map(item -> {
            if (!canModify(item, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Object>build();
            }
            item.setTitel(request.getTitel());
            item.setInhoud(request.getInhoud());
            item.setSortOrder(request.getSortOrder());
            return ResponseEntity.ok((Object) infoContentRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id, Authentication authentication) {
        return infoContentRepository.findById(id).map(item -> {
            if (!canModify(item, authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Object>build();
            }
            infoContentRepository.deleteById(id);
            return ResponseEntity.noContent().<Object>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // Hide a global TIP/FAQ for one school.
    // - bibbeheerder: scopes to their own school (schoolId param ignored).
    // - super_admin: must pass schoolId to indicate which school they're acting on.
    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/{id}/hide")
    public ResponseEntity<?> hide(@PathVariable Long id,
                                  @RequestParam(required = false) Long schoolId,
                                  Authentication authentication) {
        return infoContentRepository.findById(id).map(item -> {
            if (item.getSchool() != null) {
                return ResponseEntity.badRequest().<Object>body("Only global items can be hidden");
            }
            if (isGlobalOnlySection(item.getSectie())) {
                return ResponseEntity.badRequest().<Object>body("STAP and FEATURE items cannot be hidden");
            }
            School targetSchool = resolveScopeSchool(authentication, schoolId);
            if (targetSchool == null) {
                return ResponseEntity.badRequest().<Object>body("schoolId is required");
            }
            if (!infoContentHiddenRepository.existsBySchoolIdAndInfoContentId(targetSchool.getId(), id)) {
                infoContentHiddenRepository.save(new InfoContentHidden(targetSchool, item));
            }
            return ResponseEntity.noContent().<Object>build();
        }).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @Transactional
    @DeleteMapping("/{id}/hide")
    public ResponseEntity<?> unhide(@PathVariable Long id,
                                    @RequestParam(required = false) Long schoolId,
                                    Authentication authentication) {
        School targetSchool = resolveScopeSchool(authentication, schoolId);
        if (targetSchool == null) {
            return ResponseEntity.badRequest().body("schoolId is required");
        }
        infoContentHiddenRepository.deleteBySchoolIdAndInfoContentId(targetSchool.getId(), id);
        return ResponseEntity.noContent().build();
    }

    // ---- auth helpers ----

    private boolean isSuperAdmin(Authentication auth) {
        if (auth == null) return false;
        return auth.getAuthorities().stream()
            .anyMatch(a -> ROLE_SUPER_ADMIN.equals(a.getAuthority()));
    }

    private boolean isGlobalOnlySection(Sectie sectie) {
        return sectie == Sectie.STAP || sectie == Sectie.FEATURE;
    }

    private School resolveCallerSchool(Authentication auth) {
        AppUser caller = appUserRepository.findBySub(auth.getName())
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Gebruiker niet gevonden"));
        if (caller.getSchool() == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Geen school gevonden voor uw account");
        }
        return caller.getSchool();
    }

    private School resolveScopeSchool(Authentication auth, Long requestedSchoolId) {
        if (isSuperAdmin(auth)) {
            if (requestedSchoolId == null) return null;
            return schoolService.getByIdOrDefault(requestedSchoolId);
        }
        return resolveCallerSchool(auth);
    }

    private boolean canModify(InfoContent item, Authentication auth) {
        if (isSuperAdmin(auth)) return true;
        // bibbeheerder: only their own school's items; never globals.
        if (item.getSchool() == null) return false;
        School ownSchool = resolveCallerSchool(auth);
        return ownSchool.getId().equals(item.getSchool().getId());
    }

    public static class InfoContentRequest {
        private Long schoolId;
        private Sectie sectie;
        private String titel;
        private String inhoud;
        private int sortOrder;

        public Long getSchoolId() { return schoolId; }
        public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
        public Sectie getSectie() { return sectie; }
        public void setSectie(Sectie sectie) { this.sectie = sectie; }
        public String getTitel() { return titel; }
        public void setTitel(String titel) { this.titel = titel; }
        public String getInhoud() { return inhoud; }
        public void setInhoud(String inhoud) { this.inhoud = inhoud; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
    }
}
