package com.example.demo.controllers;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.Leeslijst;
import com.example.demo.services.LeeslijstService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leeslisten")
public class LeeslijstController {
    private final LeeslijstService leeslijstService;

    public LeeslijstController(LeeslijstService leeslijstService) {
        this.leeslijstService = leeslijstService;
    }

    /**
     * Create a new reading list (Teachers and Library Staff only)
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('leerkracht', 'bibbeheerder')")
    public Leeslijst createLeeslijst(@RequestBody CreateLeeslijstRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String userSub = auth != null ? auth.getName() : null;

        if (userSub == null || "anonymousUser".equals(userSub)) {
            throw new IllegalArgumentException("Authenticated user not found");
        }
        return leeslijstService.createLeeslijst(request, userSub);
    }

    /**
     * Get all reading lists for a school
     */
    @GetMapping("/school/{schoolId}")
    public List<Leeslijst> getLeeslisten(@PathVariable Long schoolId) {
        return leeslijstService.getLeeslisten(schoolId);
    }

    /**
     * Get reading lists for a specific class
     */
    @GetMapping("/klas/{klasId}")
    public List<Leeslijst> getLeeslistenForKlas(@PathVariable Long klasId) {
        return leeslijstService.getLeeslistenForKlas(klasId);
    }

    /**
     * Get detailed information about a reading list
     */
    @GetMapping("/{id}")
    public Leeslijst getLeeslijst(@PathVariable Long id) {
        return leeslijstService.getLeeslijst(id).orElse(null); // Assuming service returns Optional<Leeslijst>
    }

    /**
     * Update a reading list (Teachers and Library Staff only)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('leerkracht', 'bibbeheerder')")
    public Leeslijst updateLeeslijst(
        @PathVariable Long id,
        @RequestBody CreateLeeslijstRequest request
    ) {
        return leeslijstService.updateLeeslijst(id, request);
    }

    /**
     * Delete a reading list (Teachers and Library Staff only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('leerkracht', 'bibbeheerder')")
    public void deleteLeeslijst(
        @PathVariable Long id,
    ) {
        leeslijstService.deleteLeeslijst(id);
    }
}
