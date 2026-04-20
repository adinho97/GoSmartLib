package com.example.demo.services;

import com.example.demo.dto.ConfirmInviteResponse;
import com.example.demo.dto.InviteValidationResponse;
import com.example.demo.dto.TeacherDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Invite;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InviteRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class InviteService {

    private static final Logger logger = LoggerFactory.getLogger(InviteService.class);

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
        String requestedSchoolKey = normalizeSchoolKey(schoolId);
        List<AppUser> allTeachers = appUserRepository.findByRole("leerkracht");

        logger.info("Invite teacher lookup started: schoolId='{}', normalizedSchool='{}', totalLeerkrachten={}",
                schoolId, requestedSchoolKey, allTeachers.size());

        List<TeacherDto> teachers = allTeachers.stream()
                .filter(teacher -> {
                    String teacherSchoolKey = normalizeSchoolKey(teacher.getPlatform());
                    boolean match = requestedSchoolKey != null && requestedSchoolKey.equals(teacherSchoolKey);
                    if (match) {
                        logger.debug("Teacher match: userId={}, sub='{}', platform='{}', normalizedPlatform='{}'",
                                teacher.getId(), teacher.getSub(), teacher.getPlatform(), teacherSchoolKey);
                    }
                    return match;
                })
                .map(teacher -> new TeacherDto(
                        teacher.getId(),
                        teacher.getSub(),
                        teacher.getSub() // TODO: fetch naam van Smartschool API of user display name
                ))
                .collect(Collectors.toList());

        if (teachers.isEmpty()) {
            String samplePlatforms = allTeachers.stream()
                    .limit(5)
                    .map(t -> String.valueOf(t.getPlatform()))
                    .collect(Collectors.joining(", "));
            logger.warn("Invite teacher lookup returned 0 results for schoolId='{}' (normalized='{}'). Sample teacher platforms: [{}]",
                    schoolId, requestedSchoolKey, samplePlatforms);
        } else {
            logger.info("Invite teacher lookup completed: found {} matching teachers for schoolId='{}'",
                    teachers.size(), schoolId);
        }

        return teachers;
    }

    private String normalizeSchoolKey(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            String normalized = value.trim().toLowerCase();
            normalized = normalized.replaceFirst("^https?://", "");

            int slashIndex = normalized.indexOf('/');
            if (slashIndex >= 0) {
                normalized = normalized.substring(0, slashIndex);
            }

            if (normalized.startsWith("www.")) {
                normalized = normalized.substring(4);
            }

            if (normalized.endsWith(".smartschool.be")) {
                normalized = normalized.substring(0, normalized.indexOf(".smartschool.be"));
            }

            return normalized;
        } catch (Exception e) {
            logger.warn("Failed to normalize school value='{}'", value, e);
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

        String inviteSchoolKey = normalizeSchoolKey(invite.getSchoolId());
        String teacherSchoolKey = normalizeSchoolKey(teacher.getPlatform());

        if (inviteSchoolKey == null || !inviteSchoolKey.equals(teacherSchoolKey)) {
            logger.warn("Confirm invite blocked: token='{}', inviteSchool='{}' (normalized='{}'), teacherId={}, teacherPlatform='{}' (normalized='{}')",
                    token, invite.getSchoolId(), inviteSchoolKey, teacher.getId(), teacher.getPlatform(), teacherSchoolKey);
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
