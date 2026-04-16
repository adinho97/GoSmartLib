package com.example.demo.controllers;

import com.example.demo.dto.ConfirmInviteRequest;
import com.example.demo.dto.ConfirmInviteResponse;
import com.example.demo.dto.InviteValidationResponse;
import com.example.demo.dto.TeacherDto;
import com.example.demo.services.InviteService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/setup")
@CrossOrigin(origins = "*")
public class SetupController {

    private final InviteService inviteService;

    public SetupController(InviteService inviteService) {
        this.inviteService = inviteService;
    }

    @GetMapping("/invite/{token}")
    public ResponseEntity<InviteValidationResponse> validateInvite(
            @PathVariable String token,
            HttpSession session) {

        InviteValidationResponse response = inviteService.validateInvite(token);

        if (!response.isValid()) {
            return ResponseEntity.status(HttpStatus.GONE).body(response);
        }

        // Token is geldig → sla op in sessie voor volgende stap
        session.setAttribute("inviteToken", token);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/invite/{token}/teachers")
    public ResponseEntity<?> getTeachers(
            @PathVariable String token,
            HttpSession session) {

        String sessionToken = (String) session.getAttribute("inviteToken");
        if (sessionToken == null || !sessionToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Geen geldige invite sessie");
        }

        // Valideer token (double-check)
        InviteValidationResponse validation = inviteService.validateInvite(token);
        if (!validation.isValid()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(validation);
        }

        List<TeacherDto> teachers = inviteService.getTeachersBySchool(validation.getSchoolId());

        return ResponseEntity.ok(teachers);
    }

    @PostMapping("/invite/{token}/confirm")
    public ResponseEntity<?> confirmInvite(
            @PathVariable String token,
            @Valid @RequestBody ConfirmInviteRequest request,
            HttpSession session) {

        String sessionToken = (String) session.getAttribute("inviteToken");
        if (sessionToken == null || !sessionToken.equals(token)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Geen geldige invite sessie");
        }

        // Valideer token eerst
        InviteValidationResponse validation = inviteService.validateInvite(token);
        if (!validation.isValid()) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .body(validation);
        }

        // TODO: Get ingelogde user sub_id van sessie
        // Voor nu: placeholder
        String usedBySub = (String) session.getAttribute("userSub");
        if (usedBySub == null) {
            usedBySub = "unknown"; // fallback
        }

        ConfirmInviteResponse confirmResponse = inviteService.confirmInvite(
                token,
                request.getSelectedTeacherId(),
                usedBySub);

        if (confirmResponse.isSuccess()) {
            // Cleanup sessie
            session.removeAttribute("inviteToken");
            return ResponseEntity.ok(confirmResponse);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(confirmResponse);
        }
    }
}
