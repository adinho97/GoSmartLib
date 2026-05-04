package com.example.demo.controllers;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.Leeslijst;
import com.example.demo.services.LeeslijstService;
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
    public Leeslijst createLeeslijst(
        @RequestBody CreateLeeslijstRequest request,
        @RequestParam Long userId,
        @RequestParam Long schoolId
    ) {
        return leeslijstService.createLeeslijst(request, userId, schoolId);
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
    public LeeslijstDTO getLeeslijst(@PathVariable Long id) {
        return leeslijstService.getLeeslijstDTO(id);
    }

    /**
     * Update a reading list (Teachers and Library Staff only)
     */
    @PutMapping("/{id}")
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
    public void deleteLeeslijst(@PathVariable Long id) {
        leeslijstService.deleteLeeslijst(id);
    }
}
