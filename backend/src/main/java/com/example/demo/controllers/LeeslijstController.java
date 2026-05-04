package com.example.demo.controllers;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.Leeslijst;
import com.example.demo.services.LeeslijstService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public LeeslijstDTO createLeeslijst(
        @RequestBody CreateLeeslijstRequest request,
        @RequestHeader(value = "X-User-Sub", required = false) String userSub,
        @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (userSub == null) {
            throw new IllegalArgumentException("Authenticated user not found");
        }
        if (!isAuthorized(userRole)) {
            throw new org.springframework.security.access.AccessDeniedException("Geen toegang");
        }
        Leeslijst created = leeslijstService.createLeeslijst(request, userSub);
        return leeslijstService.convertToDTO(created);
    }

    /**
     * Get all reading lists for a school
     */
    @GetMapping("/school/{schoolId}")
    public List<LeeslijstDTO> getLeeslisten(@PathVariable Long schoolId) {
        return leeslijstService.getLeeslisten(schoolId)
            .stream()
            .map(leeslijstService::convertToDTO)
            .toList();
    }

    /**
     * Get reading lists for a specific class
     */
    @GetMapping("/klas/{klasId}")
    public List<LeeslijstDTO> getLeeslistenForKlas(@PathVariable Long klasId) {
        return leeslijstService.getLeeslistenForKlas(klasId)
            .stream()
            .map(leeslijstService::convertToDTO)
            .toList();
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
    public LeeslijstDTO updateLeeslijst(
        @PathVariable Long id,
        @RequestBody CreateLeeslijstRequest request,
        @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (!isAuthorized(userRole)) {
            throw new org.springframework.security.access.AccessDeniedException("Geen toegang");
        }

        Leeslijst updated = leeslijstService.updateLeeslijst(id, request);
        return leeslijstService.convertToDTO(updated);
    }

    /**
     * Delete a reading list (Teachers and Library Staff only)
     */
    @DeleteMapping("/{id}")
    public void deleteLeeslijst(
        @PathVariable Long id,
        @RequestHeader(value = "X-User-Role", required = false) String userRole
    ) {
        if (!isAuthorized(userRole)) {
            throw new org.springframework.security.access.AccessDeniedException("Geen toegang");
        }
        leeslijstService.deleteLeeslijst(id);
    }

    private boolean isAuthorized(String userRole) {
        return "leerkracht".equalsIgnoreCase(userRole) || "bibbeheerder".equalsIgnoreCase(userRole);
    }
}
