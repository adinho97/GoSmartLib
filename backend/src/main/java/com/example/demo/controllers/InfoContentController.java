package com.example.demo.controllers;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InfoContentRepository;
import com.example.demo.services.SchoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/info-content")
public class InfoContentController {

    private final InfoContentRepository infoContentRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolService schoolService;

    public InfoContentController(InfoContentRepository infoContentRepository,
                                  AppUserRepository appUserRepository,
                                  SchoolService schoolService) {
        this.infoContentRepository = infoContentRepository;
        this.appUserRepository = appUserRepository;
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<InfoContent> getAll(
            @RequestParam(required = false) Long schoolId,
            @RequestParam Sectie sectie) {
        School school = schoolService.getByIdOrDefault(schoolId);
        return infoContentRepository.findBySchoolIdAndSectieOrderBySortOrderAsc(
                school.getId(), sectie);
    }

    @GetMapping("/has-content")
    public boolean hasContent(
            @RequestParam(required = false) Long schoolId,
            @RequestParam Sectie sectie) {
        School school = schoolService.getByIdOrDefault(schoolId);
        return infoContentRepository.existsBySchoolIdAndSectie(school.getId(), sectie);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody InfoContentRequest request,
                                     @RequestHeader("X-User-Sub") String sub,
                                     @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        School school = schoolService.getByIdOrDefault(request.getSchoolId());
        InfoContent item = new InfoContent();
        item.setSchool(school);
        item.setSectie(request.getSectie());
        item.setTitel(request.getTitel());
        item.setInhoud(request.getInhoud());
        item.setSortOrder(request.getSortOrder());
        return ResponseEntity.ok(infoContentRepository.save(item));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
                                     @RequestBody InfoContentRequest request,
                                     @RequestHeader("X-User-Sub") String sub,
                                     @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        return infoContentRepository.findById(id).map(item -> {
            item.setTitel(request.getTitel());
            item.setInhoud(request.getInhoud());
            item.setSortOrder(request.getSortOrder());
            return ResponseEntity.ok(infoContentRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id,
                                     @RequestHeader("X-User-Sub") String sub,
                                     @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        if (!infoContentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        infoContentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isBibbeheerder(String sub, String role) {
        if ("bibbeheerder".equals(role)) return true;
        return appUserRepository.findBySub(sub)
                .map(u -> "bibbeheerder".equals(u.getRole()))
                .orElse(false);
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