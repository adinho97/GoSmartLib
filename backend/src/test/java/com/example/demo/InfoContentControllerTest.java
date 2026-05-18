package com.example.demo;

import com.example.demo.controllers.InfoContentController;
import com.example.demo.controllers.InfoContentController.InfoContentRequest;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.InfoContent;
import com.example.demo.entities.InfoContent.Sectie;
import com.example.demo.entities.InfoContentHidden;
import com.example.demo.entities.School;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.InfoContentHiddenRepository;
import com.example.demo.repositories.InfoContentRepository;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InfoContentController Tests")
@SuppressWarnings("null")
class InfoContentControllerTest {

    @Mock private InfoContentRepository infoContentRepository;
    @Mock private InfoContentHiddenRepository infoContentHiddenRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private SchoolService schoolService;

    @InjectMocks private InfoContentController controller;

    private static final String SUPER_SUB = "super-admin-sub";
    private static final String LIB_SUB = "bib-sub";
    private static final Long LIB_SCHOOL_ID = 1L;
    private static final Long OTHER_SCHOOL_ID = 2L;

    // ---- helpers ----

    private Authentication superAdminAuth() {
        return new UsernamePasswordAuthenticationToken(
            SUPER_SUB, null, List.of(new SimpleGrantedAuthority("ROLE_SUPER_ADMIN")));
    }

    private Authentication bibbeheerderAuth() {
        return new UsernamePasswordAuthenticationToken(
            LIB_SUB, null, List.of(new SimpleGrantedAuthority("ROLE_BIBBEHEERDER")));
    }

    private School school(long id) {
        School s = new School();
        s.setId(id);
        return s;
    }

    private InfoContent item(long id, Sectie sectie, School school, String inhoud) {
        InfoContent ic = new InfoContent();
        ReflectionTestUtils.setField(ic, "id", id);
        ic.setSectie(sectie);
        ic.setSchool(school);
        ic.setInhoud(inhoud);
        return ic;
    }

    private void stubCallerHasSchool(String sub, School school) {
        AppUser user = new AppUser();
        user.setSub(sub);
        user.setSchool(school);
        when(appUserRepository.findBySub(sub)).thenReturn(Optional.of(user));
    }

    private InfoContentRequest req(Sectie sectie, String inhoud, Long schoolId) {
        InfoContentRequest r = new InfoContentRequest();
        r.setSectie(sectie);
        r.setInhoud(inhoud);
        r.setSchoolId(schoolId);
        return r;
    }

    // ---- GET ----

    @Test
    @DisplayName("GET as super_admin with no schoolId returns globals only")
    void getReturnsGlobalsForSuperAdminWithoutSchool() {
        InfoContent global = item(10L, Sectie.STAP, null, "global step");
        when(infoContentRepository.findGlobals(Sectie.STAP)).thenReturn(List.of(global));

        List<InfoContent> result = controller.getAll(null, Sectie.STAP, superAdminAuth());

        assertEquals(1, result.size());
        assertEquals(10L, result.get(0).getId());
        verify(infoContentRepository, never()).findVisibleForSchool(any(), any());
    }

    @Test
    @DisplayName("GET as super_admin with schoolId returns school-visible items")
    void getReturnsVisibleForSchoolWhenSuperAdminPassesSchoolId() {
        School s = school(LIB_SCHOOL_ID);
        InfoContent schoolItem = item(11L, Sectie.TIP, s, "school tip");
        when(schoolService.getByIdOrDefault(LIB_SCHOOL_ID)).thenReturn(s);
        when(infoContentRepository.findVisibleForSchool(LIB_SCHOOL_ID, Sectie.TIP))
            .thenReturn(List.of(schoolItem));

        List<InfoContent> result = controller.getAll(LIB_SCHOOL_ID, Sectie.TIP, superAdminAuth());

        assertEquals(1, result.size());
        assertEquals(11L, result.get(0).getId());
    }

    @Test
    @DisplayName("GET as bibbeheerder ignores schoolId param and uses own school")
    void getIgnoresSchoolIdForBibbeheerder() {
        stubCallerHasSchool(LIB_SUB, school(LIB_SCHOOL_ID));
        when(infoContentRepository.findVisibleForSchool(LIB_SCHOOL_ID, Sectie.FAQ))
            .thenReturn(List.of());

        controller.getAll(OTHER_SCHOOL_ID, Sectie.FAQ, bibbeheerderAuth());

        verify(infoContentRepository).findVisibleForSchool(LIB_SCHOOL_ID, Sectie.FAQ);
        verify(schoolService, never()).getByIdOrDefault(any());
    }

    // ---- POST: validation ----

    @Test
    @DisplayName("POST returns 400 when sectie is missing")
    void postRejectsMissingSectie() {
        ResponseEntity<?> response = controller.create(req(null, "x", null), superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST returns 400 when inhoud is blank")
    void postRejectsBlankInhoud() {
        ResponseEntity<?> response = controller.create(req(Sectie.TIP, "   ", null), superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    // ---- POST: section/role rules ----

    @Test
    @DisplayName("POST STAP as bibbeheerder returns 403 (global-only section)")
    void postStapByBibbeheerderForbidden() {
        ResponseEntity<?> response = controller.create(
            req(Sectie.STAP, "step", null), bibbeheerderAuth());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("POST STAP as super_admin saves as global, ignoring schoolId in body")
    void postStapBySuperAdminForcedGlobal() {
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.create(
            req(Sectie.STAP, "step", LIB_SCHOOL_ID), superAdminAuth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(infoContentRepository).save(captor.capture());
        assertNull(captor.getValue().getSchool());
        assertEquals(Sectie.STAP, captor.getValue().getSectie());
        verify(schoolService, never()).getByIdOrDefault(any());
    }

    @Test
    @DisplayName("POST TIP as bibbeheerder forces school to caller's own school")
    void postTipByBibbeheerderForcesOwnSchool() {
        School own = school(LIB_SCHOOL_ID);
        stubCallerHasSchool(LIB_SUB, own);
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        controller.create(
            req(Sectie.TIP, "tip", OTHER_SCHOOL_ID /* should be ignored */),
            bibbeheerderAuth());

        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(infoContentRepository).save(captor.capture());
        assertNotNull(captor.getValue().getSchool());
        assertEquals(LIB_SCHOOL_ID, captor.getValue().getSchool().getId());
        verify(schoolService, never()).getByIdOrDefault(any());
    }

    @Test
    @DisplayName("POST TIP as super_admin with schoolId attaches that school")
    void postTipBySuperAdminWithSchool() {
        School target = school(OTHER_SCHOOL_ID);
        when(schoolService.getByIdOrDefault(OTHER_SCHOOL_ID)).thenReturn(target);
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        controller.create(req(Sectie.TIP, "tip", OTHER_SCHOOL_ID), superAdminAuth());

        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(infoContentRepository).save(captor.capture());
        assertNotNull(captor.getValue().getSchool());
        assertEquals(OTHER_SCHOOL_ID, captor.getValue().getSchool().getId());
    }

    @Test
    @DisplayName("POST FAQ as super_admin without schoolId saves as global")
    void postFaqBySuperAdminWithoutSchool() {
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        controller.create(req(Sectie.FAQ, "answer", null), superAdminAuth());

        ArgumentCaptor<InfoContent> captor = ArgumentCaptor.forClass(InfoContent.class);
        verify(infoContentRepository).save(captor.capture());
        assertNull(captor.getValue().getSchool());
        verify(schoolService, never()).getByIdOrDefault(any());
    }

    // ---- PUT ----

    @Test
    @DisplayName("PUT returns 404 when item not found")
    void putNotFound() {
        when(infoContentRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.update(
            99L, req(Sectie.TIP, "x", null), superAdminAuth());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT returns 403 when bibbeheerder edits a global")
    void putByBibbeheerderOnGlobalForbidden() {
        InfoContent global = item(5L, Sectie.TIP, null, "old");
        when(infoContentRepository.findById(5L)).thenReturn(Optional.of(global));

        ResponseEntity<?> response = controller.update(
            5L, req(Sectie.TIP, "new", null), bibbeheerderAuth());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT returns 403 when bibbeheerder edits another school's item")
    void putByBibbeheerderOnOtherSchoolForbidden() {
        InfoContent foreign = item(6L, Sectie.TIP, school(OTHER_SCHOOL_ID), "old");
        when(infoContentRepository.findById(6L)).thenReturn(Optional.of(foreign));
        stubCallerHasSchool(LIB_SUB, school(LIB_SCHOOL_ID));

        ResponseEntity<?> response = controller.update(
            6L, req(Sectie.TIP, "new", null), bibbeheerderAuth());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(infoContentRepository, never()).save(any());
    }

    @Test
    @DisplayName("PUT 200 when bibbeheerder edits own school item")
    void putByBibbeheerderOnOwnItem() {
        School own = school(LIB_SCHOOL_ID);
        InfoContent mine = item(7L, Sectie.TIP, own, "old");
        when(infoContentRepository.findById(7L)).thenReturn(Optional.of(mine));
        stubCallerHasSchool(LIB_SUB, own);
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.update(
            7L, req(Sectie.TIP, "new content", null), bibbeheerderAuth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
        InfoContent body = (InfoContent) Objects.requireNonNull(response.getBody());
        assertEquals("new content", body.getInhoud());
    }

    @Test
    @DisplayName("PUT 200 when super_admin edits any item (including STAP global)")
    void putBySuperAdminAnyItem() {
        InfoContent global = item(8L, Sectie.STAP, null, "old step");
        when(infoContentRepository.findById(8L)).thenReturn(Optional.of(global));
        when(infoContentRepository.save(any(InfoContent.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        ResponseEntity<?> response = controller.update(
            8L, req(Sectie.STAP, "new step", null), superAdminAuth());

        assertEquals(HttpStatus.OK, response.getStatusCode());
    }

    // ---- DELETE ----

    @Test
    @DisplayName("DELETE 204 when super_admin deletes any item")
    void deleteBySuperAdmin() {
        InfoContent global = item(9L, Sectie.STAP, null, "step");
        when(infoContentRepository.findById(9L)).thenReturn(Optional.of(global));

        ResponseEntity<?> response = controller.delete(9L, superAdminAuth());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(infoContentRepository).deleteById(9L);
    }

    @Test
    @DisplayName("DELETE 403 when bibbeheerder targets a global")
    void deleteByBibbeheerderOnGlobalForbidden() {
        InfoContent global = item(9L, Sectie.TIP, null, "tip");
        when(infoContentRepository.findById(9L)).thenReturn(Optional.of(global));

        ResponseEntity<?> response = controller.delete(9L, bibbeheerderAuth());

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        verify(infoContentRepository, never()).deleteById(any());
    }

    // ---- HIDE ----

    @Test
    @DisplayName("hide returns 400 when target is a school-scoped item")
    void hideRejectsSchoolItem() {
        InfoContent schoolItem = item(20L, Sectie.TIP, school(LIB_SCHOOL_ID), "tip");
        when(infoContentRepository.findById(20L)).thenReturn(Optional.of(schoolItem));

        ResponseEntity<?> response = controller.hide(20L, LIB_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentHiddenRepository, never()).save(any());
    }

    @Test
    @DisplayName("hide returns 400 for STAP global (global-only sections cannot be hidden)")
    void hideRejectsGlobalStap() {
        InfoContent global = item(21L, Sectie.STAP, null, "step");
        when(infoContentRepository.findById(21L)).thenReturn(Optional.of(global));

        ResponseEntity<?> response = controller.hide(21L, LIB_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentHiddenRepository, never()).save(any());
    }

    @Test
    @DisplayName("hide returns 400 when super_admin omits schoolId")
    void hideRequiresSchoolIdForSuperAdmin() {
        InfoContent global = item(22L, Sectie.TIP, null, "tip");
        when(infoContentRepository.findById(22L)).thenReturn(Optional.of(global));

        ResponseEntity<?> response = controller.hide(22L, null, superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentHiddenRepository, never()).save(any());
    }

    @Test
    @DisplayName("hide as super_admin saves a hidden record for the requested school")
    void hideBySuperAdminSavesRecord() {
        School target = school(LIB_SCHOOL_ID);
        InfoContent global = item(23L, Sectie.TIP, null, "tip");
        when(infoContentRepository.findById(23L)).thenReturn(Optional.of(global));
        when(schoolService.getByIdOrDefault(LIB_SCHOOL_ID)).thenReturn(target);
        when(infoContentHiddenRepository.existsBySchoolIdAndInfoContentId(LIB_SCHOOL_ID, 23L))
            .thenReturn(false);

        ResponseEntity<?> response = controller.hide(23L, LIB_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<InfoContentHidden> captor = ArgumentCaptor.forClass(InfoContentHidden.class);
        verify(infoContentHiddenRepository).save(captor.capture());
        assertEquals(LIB_SCHOOL_ID, captor.getValue().getSchool().getId());
        assertEquals(23L, captor.getValue().getInfoContent().getId());
    }

    @Test
    @DisplayName("hide is idempotent — no duplicate save when already hidden")
    void hideIsIdempotent() {
        School target = school(LIB_SCHOOL_ID);
        InfoContent global = item(24L, Sectie.TIP, null, "tip");
        when(infoContentRepository.findById(24L)).thenReturn(Optional.of(global));
        when(schoolService.getByIdOrDefault(LIB_SCHOOL_ID)).thenReturn(target);
        when(infoContentHiddenRepository.existsBySchoolIdAndInfoContentId(LIB_SCHOOL_ID, 24L))
            .thenReturn(true);

        ResponseEntity<?> response = controller.hide(24L, LIB_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(infoContentHiddenRepository, never()).save(any());
    }

    @Test
    @DisplayName("hide as bibbeheerder uses own school regardless of schoolId param")
    void hideByBibbeheerderUsesOwnSchool() {
        InfoContent global = item(25L, Sectie.FAQ, null, "answer");
        when(infoContentRepository.findById(25L)).thenReturn(Optional.of(global));
        stubCallerHasSchool(LIB_SUB, school(LIB_SCHOOL_ID));
        when(infoContentHiddenRepository.existsBySchoolIdAndInfoContentId(LIB_SCHOOL_ID, 25L))
            .thenReturn(false);

        controller.hide(25L, OTHER_SCHOOL_ID /* should be ignored */, bibbeheerderAuth());

        ArgumentCaptor<InfoContentHidden> captor = ArgumentCaptor.forClass(InfoContentHidden.class);
        verify(infoContentHiddenRepository).save(captor.capture());
        assertEquals(LIB_SCHOOL_ID, captor.getValue().getSchool().getId());
        verify(schoolService, never()).getByIdOrDefault(any());
    }

    @Test
    @DisplayName("hide returns 404 when item id is unknown")
    void hideNotFound() {
        when(infoContentRepository.findById(404L)).thenReturn(Optional.empty());

        ResponseEntity<?> response = controller.hide(404L, LIB_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
    }

    // ---- UNHIDE ----

    @Test
    @DisplayName("unhide as super_admin returns 400 when schoolId is missing")
    void unhideRequiresSchoolIdForSuperAdmin() {
        ResponseEntity<?> response = controller.unhide(30L, null, superAdminAuth());

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verify(infoContentHiddenRepository, never())
            .deleteBySchoolIdAndInfoContentId(any(), any());
    }

    @Test
    @DisplayName("unhide as bibbeheerder deletes the row for own school")
    void unhideByBibbeheerderUsesOwnSchool() {
        stubCallerHasSchool(LIB_SUB, school(LIB_SCHOOL_ID));

        ResponseEntity<?> response = controller.unhide(
            30L, OTHER_SCHOOL_ID /* should be ignored */, bibbeheerderAuth());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(infoContentHiddenRepository).deleteBySchoolIdAndInfoContentId(LIB_SCHOOL_ID, 30L);
    }

    @Test
    @DisplayName("unhide as super_admin with schoolId deletes the row for that school")
    void unhideBySuperAdminWithSchoolId() {
        when(schoolService.getByIdOrDefault(OTHER_SCHOOL_ID)).thenReturn(school(OTHER_SCHOOL_ID));

        ResponseEntity<?> response = controller.unhide(30L, OTHER_SCHOOL_ID, superAdminAuth());

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(infoContentHiddenRepository).deleteBySchoolIdAndInfoContentId(OTHER_SCHOOL_ID, 30L);
    }
}
