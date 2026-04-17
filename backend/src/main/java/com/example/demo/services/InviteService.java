package com.example.demo.services;

import com.example.demo.dto.ConfirmInviteResponse;
import com.example.demo.dto.InviteValidationResponse;
import com.example.demo.dto.TeacherDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Invite;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InviteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InviteService {

    private final InviteRepository inviteRepository;
    private final AppUserRepository appUserRepository;

    public InviteService(InviteRepository inviteRepository, AppUserRepository appUserRepository) {
        this.inviteRepository = inviteRepository;
        this.appUserRepository = appUserRepository;
    }

    public String generateInvite(String schoolId) {
        String token = UUID.randomUUID().toString();
        Invite invite = new Invite(
                token,
                schoolId,
                LocalDateTime.now().plusDays(7));
        inviteRepository.save(invite);
        return token;
    }

    public InviteValidationResponse validateInvite(String token) {
        Optional<Invite> inviteOpt = inviteRepository.findByToken(token);

        if (inviteOpt.isEmpty()) {
            return new InviteValidationResponse(false, "Link niet gevonden", null);
        }

        Invite invite = inviteOpt.get();

        if (Boolean.TRUE.equals(invite.getUsed())) {
            return new InviteValidationResponse(false, "Deze link is al gebruikt", null);
        }

        if (invite.getExpiresAt().isBefore(LocalDateTime.now())) {
            return new InviteValidationResponse(false, "Deze link is verlopen", null);
        }

        return new InviteValidationResponse(true, "Geldig", invite.getSchoolId());
    }

    public List<TeacherDto> getTeachersBySchool(String schoolId) {
        // Get all teachers with leerkracht role
        List<AppUser> allTeachers = appUserRepository.findByRole("leerkracht");

        // Filter by matching school name extracted from platform URL
        // Platform format: "https://aphogeschool.smartschool.be" → extract "aphogeschool"
        return allTeachers.stream()
                .filter(teacher -> {
                    if (teacher.getPlatform() == null) return false;
                    String platformSchool = extractSchoolFromPlatform(teacher.getPlatform());
                    return schoolId.equalsIgnoreCase(platformSchool);
                })
                .map(teacher -> new TeacherDto(
                        teacher.getId(),
                        teacher.getSub(),
                        teacher.getSub() // TODO: fetch naam van Smartschool API of user display name
                ))
                .collect(Collectors.toList());
    }

    private String extractSchoolFromPlatform(String platform) {
        // Extract school name from platform URL
        // "https://aphogeschool.smartschool.be" → "aphogeschool"
        if (platform == null || platform.isBlank()) {
            return null;
        }
        try {
            // Remove https:// and split on .smartschool.be
            return platform
                    .replaceAll("^https?://", "")
                    .split("\\.smartschool\\.be")[0];
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public ConfirmInviteResponse confirmInvite(String token, Long selectedTeacherId, String usedBySub) {
        Optional<Invite> inviteOpt = inviteRepository.findByToken(token);

        if (inviteOpt.isEmpty()) {
            return new ConfirmInviteResponse(false, "Invite niet gevonden");
        }

        Invite invite = inviteOpt.get();

        Optional<AppUser> teacherOpt = appUserRepository.findById(selectedTeacherId);
        if (teacherOpt.isEmpty()) {
            return new ConfirmInviteResponse(false, "Leerkracht niet gevonden");
        }

        AppUser teacher = teacherOpt.get();

        if (!"leerkracht".equals(teacher.getRole())) {
            return new ConfirmInviteResponse(false, "Geselecteerde user is geen leerkracht");
        }

        if (!teacher.getPlatform().equals(invite.getSchoolId())) {
            return new ConfirmInviteResponse(false, "Leerkracht is niet van de juiste school");
        }

        invite.setUsed(true);
        invite.setUsedAt(LocalDateTime.now());
        invite.setUsedBy(usedBySub);
        inviteRepository.save(invite);

        teacher.setRole("bibbeheerder");
        appUserRepository.save(teacher);

        return new ConfirmInviteResponse(true, "Bibbeheerder succesvol aangesteld!");
    }
}
