package com.example.demo.controllers;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Faq;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.FaqRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/faq")
public class FaqController {

    private final FaqRepository faqRepository;
    private final AppUserRepository appUserRepository;

    public FaqController(FaqRepository faqRepository,
                         AppUserRepository appUserRepository) {
        this.faqRepository = faqRepository;
        this.appUserRepository = appUserRepository;
    }

    // GET — toegankelijk voor iedereen (leerling leest de FAQ)
    @GetMapping
    public List<Faq> getAllFaqs() {
        return faqRepository.findAllByOrderBySortOrderAsc();
    }

    // POST — alleen bibbeheerder
    @PostMapping
    public ResponseEntity<?> createFaq(@RequestBody Faq faq,
                                       @RequestHeader("X-User-Sub") String sub) {
        if (!isBibbeheerder(sub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        return ResponseEntity.ok(faqRepository.save(faq));
    }

    // PUT — alleen bibbeheerder
    @PutMapping("/{id}")
    public ResponseEntity<?> updateFaq(@PathVariable Long id,
                                       @RequestBody Faq updated,
                                       @RequestHeader("X-User-Sub") String sub) {
        if (!isBibbeheerder(sub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        return faqRepository.findById(id).map(faq -> {
            faq.setQuestion(updated.getQuestion());
            faq.setAnswer(updated.getAnswer());
            faq.setSortOrder(updated.getSortOrder());
            return ResponseEntity.ok(faqRepository.save(faq));
        }).orElse(ResponseEntity.notFound().build());
    }

    // DELETE — alleen bibbeheerder
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFaq(@PathVariable Long id,
                                       @RequestHeader("X-User-Sub") String sub) {
        if (!isBibbeheerder(sub)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Geen toegang.");
        }
        if (!faqRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        faqRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private boolean isBibbeheerder(String sub) {
        Optional<AppUser> user = appUserRepository.findBySub(sub);
        return user.map(u -> "bibbeheerder".equals(u.getRole())).orElse(false);
    }
}