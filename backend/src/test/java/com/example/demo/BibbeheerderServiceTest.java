package com.example.demo;

import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.services.BibbeheerderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class BibbeheerderServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private KlasRepository klasRepository;

    private BibbeheerderService service;

    @BeforeEach
    void setUp() {
        service = new BibbeheerderService(appUserRepository, klasRepository);
    }

    // --- getLeerkrachtenInOwnSchool ---

    @Test
    void getLeerkrachtenInOwnSchool_shouldReturnMappedLeerkrachten() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        AppUser leerkracht = new AppUser();
        leerkracht.setId(10L);
        leerkracht.setSub("leerkracht-sub");
        leerkracht.setRole("leerkracht");
        leerkracht.setActive(true);
        leerkracht.setSchool(school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findBySchool_IdAndRole(1L, "leerkracht")).thenReturn(List.of(leerkracht));

        List<AdminUserListItem> result = service.getLeerkrachtenInOwnSchool("bib-sub");

        assertEquals(1, result.size());
        AdminUserListItem item = result.get(0);
        assertEquals(10L, item.getId());
        assertEquals("leerkracht-sub", item.getSub());
        assertEquals("leerkracht", item.getRole());
        assertTrue(item.isActive());
        assertNull(item.getKlasNaam());
    }

    @Test
    void getLeerkrachtenInOwnSchool_shouldIncludeKlasNaamWhenPresent() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        Klas klas = new Klas();
        klas.setNaam("6A");

        AppUser leerkracht = new AppUser();
        leerkracht.setId(11L);
        leerkracht.setSub("leerkracht-sub-2");
        leerkracht.setRole("leerkracht");
        leerkracht.setActive(true);
        leerkracht.setSchool(school);
        leerkracht.setKlas(klas);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findBySchool_IdAndRole(1L, "leerkracht")).thenReturn(List.of(leerkracht));

        List<AdminUserListItem> result = service.getLeerkrachtenInOwnSchool("bib-sub");

        assertEquals("6A", result.get(0).getKlasNaam());
    }

    @Test
    void getLeerkrachtenInOwnSchool_shouldReturnEmptyListWhenNoLeerkrachten() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findBySchool_IdAndRole(1L, "leerkracht")).thenReturn(List.of());

        List<AdminUserListItem> result = service.getLeerkrachtenInOwnSchool("bib-sub");

        assertTrue(result.isEmpty());
    }

    @Test
    void getLeerkrachtenInOwnSchool_shouldThrowForbiddenWhenCallerNotFound() {
        when(appUserRepository.findBySub("unknown-sub")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getLeerkrachtenInOwnSchool("unknown-sub"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("NO_SCHOOL", ex.getCode());
    }

    @Test
    void getLeerkrachtenInOwnSchool_shouldThrowForbiddenWhenCallerHasNoSchool() {
        AppUser caller = new AppUser();
        caller.setSub("bib-sub");
        caller.setSchool(null);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.getLeerkrachtenInOwnSchool("bib-sub"));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("NO_SCHOOL", ex.getCode());
    }

    // --- promoteLeerkrachtToBibbeheerder ---

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldSetRoleAndReturnItem() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setSub("target-sub");
        target.setRole("leerkracht");
        target.setActive(true);
        target.setSchool(school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserListItem result = service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L);

        assertEquals("bibbeheerder", result.getRole());
        assertEquals(20L, result.getId());
        assertEquals("target-sub", result.getSub());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldSaveWithBibbeheerderRole() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setSub("target-sub");
        target.setRole("leerkracht");
        target.setSchool(school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        assertEquals("bibbeheerder", captor.getValue().getRole());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowNotFoundWhenTargetMissing() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 99L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowForbiddenWhenTargetBelongsToDifferentSchool() {
        School callerSchool = makeSchool(1L);
        School otherSchool = makeSchool(2L);

        AppUser caller = makeCaller("bib-sub", callerSchool);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setRole("leerkracht");
        target.setSchool(otherSchool);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowForbiddenWhenTargetHasNoSchool() {
        School callerSchool = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", callerSchool);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setRole("leerkracht");
        target.setSchool(null);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowBadRequestWhenTargetAlreadyBibbeheerder() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setRole("bibbeheerder");
        target.setSchool(school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_ROLE_TRANSITION", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowBadRequestWhenTargetIsLeerling() {
        School school = makeSchool(1L);
        AppUser caller = makeCaller("bib-sub", school);

        AppUser target = new AppUser();
        target.setId(20L);
        target.setRole("leerling");
        target.setSchool(school);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));
        when(appUserRepository.findById(20L)).thenReturn(Optional.of(target));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_ROLE_TRANSITION", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowForbiddenWhenCallerNotFound() {
        when(appUserRepository.findBySub("unknown-sub")).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("unknown-sub", 20L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("NO_SCHOOL", ex.getCode());
    }

    @Test
    void promoteLeerkrachtToBibbeheerder_shouldThrowForbiddenWhenCallerHasNoSchool() {
        AppUser caller = new AppUser();
        caller.setSub("bib-sub");
        caller.setSchool(null);

        when(appUserRepository.findBySub("bib-sub")).thenReturn(Optional.of(caller));

        ApiException ex = assertThrows(ApiException.class,
                () -> service.promoteLeerkrachtToBibbeheerder("bib-sub", 20L));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("NO_SCHOOL", ex.getCode());
    }

    // --- helpers ---

    private School makeSchool(Long id) {
        School school = new School();
        school.setId(id);
        return school;
    }

    private AppUser makeCaller(String sub, School school) {
        AppUser caller = new AppUser();
        caller.setSub(sub);
        caller.setRole("bibbeheerder");
        caller.setActive(true);
        caller.setSchool(school);
        return caller;
    }
}
