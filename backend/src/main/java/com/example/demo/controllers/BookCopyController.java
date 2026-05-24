package com.example.demo.controllers;

import com.example.demo.dto.CopyDto;
import com.example.demo.dto.UpdateCopyStateRequest;
import com.example.demo.services.BookCopyService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/exemplaren")
public class BookCopyController {

    private final BookCopyService bookCopyService;

    public BookCopyController(BookCopyService bookCopyService) {
        this.bookCopyService = bookCopyService;
    }

    @GetMapping("/boek/{bookId}/summary")
    public ResponseEntity<Map<String, Long>> getSummary(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookCopyService.getSummary(bookId));
    }

    @GetMapping("/boek/{bookId}")
    public ResponseEntity<List<CopyDto>> getCopies(@PathVariable Long bookId) {
        return ResponseEntity.ok(bookCopyService.getCopiesForBook(bookId));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PostMapping("/boek/{bookId}")
    public ResponseEntity<CopyDto> addCopy(@PathVariable Long bookId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookCopyService.addCopy(bookId));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @PatchMapping("/{id}")
    public ResponseEntity<CopyDto> updateCopyState(
            @PathVariable Long id,
            @RequestBody UpdateCopyStateRequest request) {
        return ResponseEntity.ok(bookCopyService.updateCopyState(id, request));
    }

    @PreAuthorize("hasAnyRole('BIBBEHEERDER', 'SUPER_ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCopy(@PathVariable Long id) {
        bookCopyService.deleteCopy(id);
        return ResponseEntity.noContent().build();
    }
}
