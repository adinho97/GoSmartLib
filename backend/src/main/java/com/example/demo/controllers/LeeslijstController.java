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

    private String requireUserSub(String userSub) {
        if (userSub == null || userSub.isBlank()) {
            throw new IllegalArgumentException("X-User-Sub header is required");
        }
        return userSub;
    }

    @PostMapping
    public ResponseEntity<LeeslijstDTO> createLeeslijst(
            @RequestBody CreateLeeslijstRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub,
            @RequestHeader(value = "X-User-Name", required = false) String userName) {
        Leeslijst created = leeslijstService.createLeeslijst(request, requireUserSub(userSub), userName);
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

    @PutMapping("/{id}")
    public ResponseEntity<LeeslijstDTO> updateLeeslijst(
            @PathVariable Long id,
            @RequestBody CreateLeeslijstRequest request,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        requireUserSub(userSub);
        Leeslijst updated = leeslijstService.updateLeeslijst(id, request);
        return ResponseEntity.ok(leeslijstService.convertToDTO(updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteLeeslijst(
            @PathVariable Long id,
            @RequestHeader(value = "X-User-Sub", required = false) String userSub) {
        requireUserSub(userSub);
        leeslijstService.deleteLeeslijst(id);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(ex.getMessage());
    }
}
