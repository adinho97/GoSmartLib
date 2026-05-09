package com.example.demo.controllers;

import com.example.demo.dto.CreateLeeslijstRequest;
import com.example.demo.dto.LeeslijstDTO;
import com.example.demo.entities.Leeslijst;
import com.example.demo.services.LeeslijstService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leeslisten")
public class LeeslijstController {

    private final LeeslijstService leeslijstService;

    public LeeslijstController(LeeslijstService leeslijstService) {
        this.leeslijstService = leeslijstService;
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping
    public ResponseEntity<LeeslijstDTO> createLeeslijst(
            @RequestBody CreateLeeslijstRequest request,
            Authentication authentication,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        Leeslijst created = leeslijstService.createLeeslijst(request, authentication.getName(), userName);
        return ResponseEntity.status(HttpStatus.CREATED).body(leeslijstService.convertToDTO(created));
    }

    @GetMapping("/{id}")
    public ResponseEntity<LeeslijstDTO> getLeeslijst(@PathVariable Long id) {
        return leeslijstService.getLeeslijst(id)
                .map(leeslijstService::convertToDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/school/{schoolId}")
    public ResponseEntity<List<LeeslijstDTO>> getLeeslistenBySchool(@PathVariable Long schoolId) {
        List<LeeslijstDTO> result = leeslijstService.getLeeslisten(schoolId)
                .stream()
                .map(leeslijstService::convertToDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @GetMapping("/klas/{klasId}")
    public ResponseEntity<List<LeeslijstDTO>> getLeeslistenForKlas(@PathVariable Long klasId) {
        List<LeeslijstDTO> result = leeslijstService.getLeeslistenForKlas(klasId)
                .stream()
                .map(leeslijstService::convertToDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @GetMapping("/mijn")
    public ResponseEntity<List<LeeslijstDTO>> getMijnLeeslisten(Authentication authentication) {
        List<LeeslijstDTO> result = leeslijstService.getLeeslistenForUser(authentication.getName())
                .stream()
                .map(leeslijstService::convertToDTO)
                .toList();
        return ResponseEntity.ok(result);
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<LeeslijstDTO> updateLeeslijst(
            @PathVariable Long id,
            @RequestBody CreateLeeslijstRequest request,
            Authentication authentication) {
        Leeslijst updated = leeslijstService.updateLeeslijst(id, request, authentication.getName());
        return ResponseEntity.ok(leeslijstService.convertToDTO(updated));
    }

    @PreAuthorize("hasAnyRole('LEERKRACHT', 'BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeeslijst(
            @PathVariable Long id,
            Authentication authentication) {
        leeslijstService.deleteLeeslijst(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<String> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
