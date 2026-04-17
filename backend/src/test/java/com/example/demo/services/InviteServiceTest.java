package com.example.demo.services;

import com.example.demo.dto.ConfirmInviteResponse;
import com.example.demo.dto.InviteValidationResponse;
import com.example.demo.dto.TeacherDto;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Invite;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InviteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InviteServiceTest {

    @Mock
    private InviteRepository inviteRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private InviteService inviteService;

    private String testToken;
    private String schoolId;
    private Invite testInvite;

    @BeforeEach
    void setUp() {
        schoolId = "aphogeschool.smartschool.be";
        testToken = "test-token-123";
        testInvite = new Invite(testToken, schoolId, LocalDateTime.now().plusDays(7));
    }

    // ===== generateInvite() Tests =====

    @Test
    void generateInvite_shouldCreateTokenWithExpiry() {
        // Arrange
        when(inviteRepository.save(any(Invite.class))).thenReturn(testInvite);

        // Act
        String token = inviteService.generateInvite(schoolId);

        // Assert
        assertNotNull(token);
        assertEquals(36, token.length()); // UUID format
        verify(inviteRepository, times(1)).save(any(Invite.class));
    }

    @Test
    void generateInvite_shouldSetExpiryTo7Days() {
        // Arrange
        when(inviteRepository.save(any(Invite.class))).thenAnswer(invocation -> {
            Invite invite = invocation.getArgument(0);
            return invite;
        });

        // Act
        inviteService.generateInvite(schoolId);

        // Assert
        verify(inviteRepository, times(1)).save(argThat(invite ->
            invite.getExpiresAt().isAfter(LocalDateTime.now().plusDays(6)) &&
            invite.getExpiresAt().isBefore(LocalDateTime.now().plusDays(8))
        ));
    }

    // ===== validateInvite() Tests =====

    @Test
    void validateInvite_shouldReturnValidForValidToken() {
        // Arrange
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));

        // Act
        InviteValidationResponse response = inviteService.validateInvite(testToken);

        // Assert
        assertTrue(response.isValid());
        assertEquals(schoolId, response.getSchoolId());
    }

    @Test
    void validateInvite_shouldRejectExpiredToken() {
        // Arrange
        Invite expiredInvite = new Invite(testToken, schoolId, LocalDateTime.now().minusHours(1));
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(expiredInvite));

        // Act
        InviteValidationResponse response = inviteService.validateInvite(testToken);

        // Assert
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("verlopen"));
    }

    @Test
    void validateInvite_shouldRejectAlreadyUsedToken() {
        // Arrange
        testInvite.setUsed(true);
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));

        // Act
        InviteValidationResponse response = inviteService.validateInvite(testToken);

        // Assert
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("al gebruikt"));
    }

    @Test
    void validateInvite_shouldRejectNonExistentToken() {
        // Arrange
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // Act
        InviteValidationResponse response = inviteService.validateInvite(testToken);

        // Assert
        assertFalse(response.isValid());
        assertTrue(response.getMessage().contains("niet gevonden"));
    }

    // ===== getTeachersBySchool() Tests =====

    @Test
    void getTeachersBySchool_shouldReturnTeachersWithLeerkrachtRole() {
        // Arrange
        AppUser teacher1 = new AppUser();
        teacher1.setId(1L);
        teacher1.setSub("teacher-sub-1");
        teacher1.setRole("leerkracht");
        teacher1.setPlatform(schoolId);

        AppUser teacher2 = new AppUser();
        teacher2.setId(2L);
        teacher2.setSub("teacher-sub-2");
        teacher2.setRole("leerkracht");
        teacher2.setPlatform(schoolId);

        when(appUserRepository.findByRoleAndPlatform("leerkracht", schoolId))
            .thenReturn(Arrays.asList(teacher1, teacher2));

        // Act
        List<TeacherDto> teachers = inviteService.getTeachersBySchool(schoolId);

        // Assert
        assertEquals(2, teachers.size());
        assertEquals("teacher-sub-1", teachers.get(0).getSub());
        assertEquals("teacher-sub-2", teachers.get(1).getSub());
    }

    @Test
    void getTeachersBySchool_shouldReturnEmptyListIfNoTeachers() {
        // Arrange
        when(appUserRepository.findByRoleAndPlatform("leerkracht", schoolId))
            .thenReturn(Arrays.asList());

        // Act
        List<TeacherDto> teachers = inviteService.getTeachersBySchool(schoolId);

        // Assert
        assertTrue(teachers.isEmpty());
    }

    @Test
    void getTeachersBySchool_shouldFilterByPlatform() {
        // Arrange
        when(appUserRepository.findByRoleAndPlatform("leerkracht", schoolId))
            .thenReturn(Arrays.asList());

        // Act
        inviteService.getTeachersBySchool(schoolId);

        // Assert
        verify(appUserRepository, times(1)).findByRoleAndPlatform("leerkracht", schoolId);
    }

    // ===== confirmInvite() Tests =====

    @Test
    void confirmInvite_shouldMarkTokenUsedAndPromoteTeacher() {
        // Arrange
        AppUser teacher = new AppUser();
        teacher.setId(1L);
        teacher.setSub("teacher-sub-1");
        teacher.setRole("leerkracht");
        teacher.setPlatform(schoolId);

        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(teacher));
        when(appUserRepository.save(any(AppUser.class))).thenReturn(teacher);
        when(inviteRepository.save(any(Invite.class))).thenReturn(testInvite);

        // Act
        ConfirmInviteResponse response = inviteService.confirmInvite(testToken, 1L, "admin-sub");

        // Assert
        assertTrue(response.isSuccess());
        verify(inviteRepository, times(1)).save(argThat(invite ->
            invite.getUsed() && invite.getUsedBy().equals("admin-sub")
        ));
        verify(appUserRepository, times(1)).save(argThat(user ->
            user.getRole().equals("bibbeheerder")
        ));
    }

    @Test
    void confirmInvite_shouldFailIfTokenNotFound() {
        // Arrange
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.empty());

        // Act
        ConfirmInviteResponse response = inviteService.confirmInvite(testToken, 1L, "admin-sub");

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("niet gevonden"));
    }

    @Test
    void confirmInvite_shouldFailIfTeacherNotFound() {
        // Arrange
        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));
        when(appUserRepository.findById(1L)).thenReturn(Optional.empty());

        // Act
        ConfirmInviteResponse response = inviteService.confirmInvite(testToken, 1L, "admin-sub");

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("niet gevonden"));
    }

    @Test
    void confirmInvite_shouldFailIfTeacherIsNotLeerkracht() {
        // Arrange
        AppUser student = new AppUser();
        student.setId(1L);
        student.setRole("leerling");

        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(student));

        // Act
        ConfirmInviteResponse response = inviteService.confirmInvite(testToken, 1L, "admin-sub");

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("geen leerkracht"));
    }

    @Test
    void confirmInvite_shouldFailIfTeacherFromDifferentSchool() {
        // Arrange
        AppUser teacher = new AppUser();
        teacher.setId(1L);
        teacher.setRole("leerkracht");
        teacher.setPlatform("other-school.smartschool.be"); // Different school

        when(inviteRepository.findByToken(testToken)).thenReturn(Optional.of(testInvite));
        when(appUserRepository.findById(1L)).thenReturn(Optional.of(teacher));

        // Act
        ConfirmInviteResponse response = inviteService.confirmInvite(testToken, 1L, "admin-sub");

        // Assert
        assertFalse(response.isSuccess());
        assertTrue(response.getMessage().contains("niet van de juiste school"));
    }
}
