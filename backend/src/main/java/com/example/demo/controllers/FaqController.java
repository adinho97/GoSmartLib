package com.example.demo.controllers;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Faq;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.FaqRepository;
import com.example.demo.services.SchoolService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    private final FaqRepository faqRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolService schoolService;

    public FaqController(FaqRepository faqRepository,
                         AppUserRepository appUserRepository,
                         SchoolService schoolService) {
        this.faqRepository = faqRepository;
        this.appUserRepository = appUserRepository;
        this.schoolService = schoolService;
    }

    @GetMapping
    public List<Faq> getAllFaqs(@RequestParam(required = false) Long schoolId) {
        School school = schoolService.getByIdOrDefault(schoolId);
        return faqRepository.findBySchoolIdOrderBySortOrderAsc(school.getId());
    }

    @PostMapping
    public ResponseEntity<?> createFaq(@RequestBody FaqRequest request,
                                       @RequestHeader("X-User-Sub") String sub,
                                       @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        School school = schoolService.getByIdOrDefault(request.getSchoolId());
        Faq faq = new Faq();
        faq.setQuestion(request.getQuestion());
        faq.setAnswer(request.getAnswer());
        faq.setSortOrder(request.getSortOrder());
        faq.setSchool(school);
        return ResponseEntity.ok(faqRepository.save(faq));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateFaq(@PathVariable Long id,
                                       @RequestBody FaqRequest request,
                                       @RequestHeader("X-User-Sub") String sub,
                                       @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        return faqRepository.findById(id).map(faq -> {
            faq.setQuestion(request.getQuestion());
            faq.setAnswer(request.getAnswer());
            faq.setSortOrder(request.getSortOrder());
            if (request.getSchoolId() != null) {
                School school = schoolService.getByIdOrDefault(request.getSchoolId());
                faq.setSchool(school);
            }
            return ResponseEntity.ok(faqRepository.save(faq));
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFaq(@PathVariable Long id,
                                       @RequestHeader("X-User-Sub") String sub,
                                       @RequestHeader("X-User-Role") String role) {
        if (!isBibbeheerder(sub, role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        if (!faqRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        faqRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isBibbeheerder(String sub, String role) {
        if ("bibbeheerder".equals(role)) {
            return true;
        }
        return appUserRepository.findBySub(sub)
                .map(u -> "bibbeheerder".equals(u.getRole()))
                .orElse(false);
    }

    // Inner DTO class
    public static class FaqRequest {
        private String question;
        private String answer;
        private int sortOrder;
        private Long schoolId;

        public String getQuestion() { return question; }
        public void setQuestion(String question) { this.question = question; }
        public String getAnswer() { return answer; }
        public void setAnswer(String answer) { this.answer = answer; }
        public int getSortOrder() { return sortOrder; }
        public void setSortOrder(int sortOrder) { this.sortOrder = sortOrder; }
        public Long getSchoolId() { return schoolId; }
        public void setSchoolId(Long schoolId) { this.schoolId = schoolId; }
    }
}