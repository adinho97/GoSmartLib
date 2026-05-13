package com.example.demo.controllers;

import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.entities.School;
import com.example.demo.repositories.InfoContentRepository;
import com.example.demo.services.SchoolService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/info-content")
@SuppressWarnings("null")
public class InfoContentController {

    private final InfoContentRepository infoContentRepository;
    private final SchoolService schoolService;

    public InfoContentController(InfoContentRepository infoContentRepository,
            SchoolService schoolService) {
        this.infoContentRepository = infoContentRepository;
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<InfoContent> getAll(
            @RequestParam(required = false) Long schoolId,
            @RequestParam Sectie sectie) {
        School school = schoolService.getByIdOrDefault(schoolId);
        Long resolvedSchoolId = Objects.requireNonNull(school.getId(), "schoolId is required");
        return infoContentRepository.findBySchoolIdAndSectieOrderBySortOrderAsc(
            resolvedSchoolId, sectie);
    }

    @GetMapping("/has-content")
    public boolean hasContent(
            @RequestParam(required = false) Long schoolId,
            @RequestParam Sectie sectie) {
        School school = schoolService.getByIdOrDefault(schoolId);
        Long resolvedSchoolId = Objects.requireNonNull(school.getId(), "schoolId is required");
        return infoContentRepository.existsBySchoolIdAndSectie(resolvedSchoolId, sectie);
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody InfoContentRequest request) {
        if (request.getSchoolId() == null) {
            return ResponseEntity.badRequest().build();
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

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id,
            @RequestBody InfoContentRequest request) {
        Objects.requireNonNull(id, "id is required");
        return infoContentRepository.findById(id).map(item -> {
            item.setTitel(request.getTitel());
            item.setInhoud(request.getInhoud());
            item.setSortOrder(request.getSortOrder());
            return ResponseEntity.ok(infoContentRepository.save(item));
        }).orElse(ResponseEntity.notFound().build());
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!infoContentRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        infoContentRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    public static class InfoContentRequest {
        private Long schoolId;
        private Sectie sectie;
        private String titel;
        private String inhoud;
        private int sortOrder;

        public Long getSchoolId() {
            return schoolId;
        }

        public void setSchoolId(Long schoolId) {
            this.schoolId = schoolId;
        }

        public Sectie getSectie() {
            return sectie;
        }

        public void setSectie(Sectie sectie) {
            this.sectie = sectie;
        }

        public String getTitel() {
            return titel;
        }

        public void setTitel(String titel) {
            this.titel = titel;
        }

        public String getInhoud() {
            return inhoud;
        }

        public void setInhoud(String inhoud) {
            this.inhoud = inhoud;
        }

        public int getSortOrder() {
            return sortOrder;
        }

        public void setSortOrder(int sortOrder) {
            this.sortOrder = sortOrder;
        }
    }
}