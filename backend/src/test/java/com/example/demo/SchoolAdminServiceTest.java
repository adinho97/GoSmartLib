package com.example.demo;

import com.example.demo.repositories.ClassReadingListItemRepository;
import com.example.demo.dto.admin.school.CreateSchoolRequest;
import com.example.demo.dto.admin.school.CreateSchoolResponse;
import com.example.demo.dto.admin.school.KlasListItem;
import com.example.demo.dto.admin.school.SchoolDashboardItemResponse;
import com.example.demo.dto.admin.school.SchoolDetailResponse;
import com.example.demo.dto.admin.school.UpdateSchoolInfoRequest;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.BookRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.LoanRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.repositories.WishlistRepository;
import com.example.demo.services.SchoolAdminService;
import com.example.demo.services.SchoolAdminValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class SchoolAdminServiceTest {

    @Mock private SchoolRepository schoolRepository;
    @Mock private KlasRepository klasRepository;
    @Mock private AppUserRepository appUserRepository;
    @Mock private SchoolAdminValidationService validationService;
    @Mock private BookRepository bookRepository;
    @Mock private LoanRepository loanRepository;
    @Mock private WishlistRepository wishlistRepository;
    @Mock private ClassReadingListItemRepository classReadingListItemRepository;

    private SchoolAdminService service;

    @BeforeEach
    void setUp() {
        service = new SchoolAdminService(
                schoolRepository, klasRepository, appUserRepository,
                validationService, bookRepository, loanRepository,
                wishlistRepository, classReadingListItemRepository);
    }

    // --- addSchool ---

    @Test
    void addSchool_shouldNormalizeSubdomainBuildUrlAndSaveWithPendingStatus() {
        CreateSchoolRequest request = new CreateSchoolRequest();
        request.setSubdomain("  MySchool  ");
        request.setNaam("  GO! Atheneum  ");
        request.setAdres("  Schoolstraat 1  ");
        request.setLatitude(51.2);
        request.setLongitude(4.4);

        when(validationService.normalizeSubdomain("  MySchool  ")).thenReturn("myschool");
        when(validationService.buildSmartschoolUrl("myschool")).thenReturn("https://myschool.smartschool.be");

        School saved = makeSchool(1L, "myschool", "https://myschool.smartschool.be");
        saved.setNaam("GO! Atheneum");
        saved.setAdres("Schoolstraat 1");
        saved.setLatitude(51.2);
        saved.setLongitude(4.4);
        when(schoolRepository.save(any(School.class))).thenReturn(saved);

        CreateSchoolResponse response = service.addSchool(request);

        verify(validationService).normalizeSubdomain("  MySchool  ");
        verify(validationService).assertSubdomainAvailable("myschool");

        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        School persisted = captor.getValue();
        assertEquals("myschool", persisted.getSubdomein());
        assertEquals("https://myschool.smartschool.be", persisted.getSmartschoolUrl());
        assertEquals("GO! Atheneum", persisted.getNaam());
        assertEquals("Schoolstraat 1", persisted.getAdres());
        assertEquals(SchoolStatus.PENDING, persisted.getStatus());

        assertEquals(1L, response.getId());
        assertEquals("myschool", response.getSubdomain());
    }

    @Test
    void addSchool_shouldTrimNaamAndAdres() {
        CreateSchoolRequest request = new CreateSchoolRequest();
        request.setSubdomain("school");
        request.setNaam("  Trimmed Name  ");
        request.setAdres("  Trimmed Adres  ");

        when(validationService.normalizeSubdomain("school")).thenReturn("school");
        when(validationService.buildSmartschoolUrl("school")).thenReturn("https://school.smartschool.be");
        when(schoolRepository.save(any(School.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.addSchool(request);

        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertEquals("Trimmed Name", captor.getValue().getNaam());
        assertEquals("Trimmed Adres", captor.getValue().getAdres());
    }

    @Test
    void addSchool_shouldAllowNullNaamAndAdres() {
        CreateSchoolRequest request = new CreateSchoolRequest();
        request.setSubdomain("school");
        request.setNaam(null);
        request.setAdres(null);

        when(validationService.normalizeSubdomain("school")).thenReturn("school");
        when(validationService.buildSmartschoolUrl("school")).thenReturn("https://school.smartschool.be");
        when(schoolRepository.save(any(School.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        service.addSchool(request);

        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertNull(captor.getValue().getNaam());
        assertNull(captor.getValue().getAdres());
    }

    // --- getAllSchools ---

    @Test
    void getAllSchools_shouldReturnMappedDashboardItems() {
        School a = makeSchool(1L, "schoola", "https://schoola.smartschool.be");
        a.setNaam("School A");
        a.setStatus(SchoolStatus.ACTIVE);
        School b = makeSchool(2L, "schoolb", "https://schoolb.smartschool.be");
        b.setStatus(SchoolStatus.PENDING);

        when(schoolRepository.findAll()).thenReturn(List.of(a, b));
        when(appUserRepository.countBySchool_Id(1L)).thenReturn(5L);
        when(klasRepository.countBySchool_Id(1L)).thenReturn(3L);
        when(appUserRepository.countBySchool_Id(2L)).thenReturn(0L);
        when(klasRepository.countBySchool_Id(2L)).thenReturn(0L);

        List<SchoolDashboardItemResponse> result = service.getAllSchools();

        assertEquals(2, result.size());
        SchoolDashboardItemResponse first = result.get(0);
        assertEquals(1L, first.getId());
        assertEquals("School A", first.getNaam());
        assertEquals("schoola", first.getSubdomain());
        assertEquals(SchoolStatus.ACTIVE, first.getStatus());
        assertEquals(5L, first.getUserCount());
        assertEquals(3L, first.getKlasCount());
    }

    @Test
    void getAllSchools_shouldReturnEmptyListWhenNoSchools() {
        when(schoolRepository.findAll()).thenReturn(List.of());
        assertTrue(service.getAllSchools().isEmpty());
    }

    // --- getSchoolDetail ---

    @Test
    void getSchoolDetail_shouldReturnDetailWithCounts() {
        School school = makeSchool(7L, "demo", "https://demo.smartschool.be");
        school.setNaam("Demo School");
        school.setStatus(SchoolStatus.ACTIVE);
        school.setAangemaaktOp(LocalDateTime.of(2025, 1, 1, 0, 0));

        when(schoolRepository.findById(7L)).thenReturn(Optional.of(school));
        when(appUserRepository.countBySchool_Id(7L)).thenReturn(10L);
        when(klasRepository.countBySchool_Id(7L)).thenReturn(4L);
        when(bookRepository.countBySchool_Id(7L)).thenReturn(200L);
        when(loanRepository.countByCopy_Book_School_IdAndReturnedAtIsNull(7L)).thenReturn(12L);
        when(wishlistRepository.countByBook_School_Id(7L)).thenReturn(8L);
        when(classReadingListItemRepository.countBySchoolId(7L)).thenReturn(3L);

        SchoolDetailResponse detail = service.getSchoolDetail(7L);

        assertEquals(7L, detail.getId());
        assertEquals("Demo School", detail.getNaam());
        assertEquals("demo", detail.getSubdomain());
        assertEquals(SchoolStatus.ACTIVE, detail.getStatus());
        assertEquals(10L, detail.getUserCount());
        assertEquals(4L, detail.getKlasCount());
        assertEquals(200L, detail.getBookCount());
        assertEquals(12L, detail.getActiveLoansCount());
        assertEquals(8L, detail.getWishlistCount());
        assertEquals(3L, detail.getClassReadingListCount());
    }

    @Test
    void getSchoolDetail_shouldThrowNotFoundWhenSchoolMissing() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getSchoolDetail(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("SCHOOL_NOT_FOUND", ex.getCode());
    }

    // --- updateSchoolInfo ---

    @Test
    void updateSchoolInfo_shouldUpdateFieldsAndReturnDetail() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        stubDetailCounts(1L);

        UpdateSchoolInfoRequest request = new UpdateSchoolInfoRequest();
        request.setNaam("  Nieuw Naam  ");
        request.setAdres("  Nieuw Adres  ");
        request.setLatitude(50.9);
        request.setLongitude(4.3);

        SchoolDetailResponse result = service.updateSchoolInfo(1L, request);

        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        School saved = captor.getValue();
        assertEquals("Nieuw Naam", saved.getNaam());
        assertEquals("Nieuw Adres", saved.getAdres());
        assertEquals(50.9, saved.getLatitude());
        assertEquals(4.3, saved.getLongitude());
        assertNotNull(result);
    }

    @Test
    void updateSchoolInfo_shouldClearNaamWhenBlank() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        school.setNaam("Oud Naam");

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        stubDetailCounts(1L);

        UpdateSchoolInfoRequest request = new UpdateSchoolInfoRequest();
        request.setNaam("   ");

        service.updateSchoolInfo(1L, request);

        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertNull(captor.getValue().getNaam());
    }

    @Test
    void updateSchoolInfo_shouldThrowNotFoundWhenSchoolMissing() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateSchoolInfoRequest request = new UpdateSchoolInfoRequest();
        ApiException ex = assertThrows(ApiException.class, () -> service.updateSchoolInfo(99L, request));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- updateSchoolStatus ---

    @Test
    void updateSchoolStatus_shouldUpdateStatusAndReturnDetail() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        school.setStatus(SchoolStatus.PENDING);

        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));
        when(schoolRepository.save(any(School.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        stubDetailCounts(1L);

        SchoolDetailResponse result = service.updateSchoolStatus(1L, SchoolStatus.ACTIVE);

        verify(validationService).validateUpdatableStatus(SchoolStatus.ACTIVE);
        ArgumentCaptor<School> captor = ArgumentCaptor.forClass(School.class);
        verify(schoolRepository).save(captor.capture());
        assertEquals(SchoolStatus.ACTIVE, captor.getValue().getStatus());
        assertNotNull(result);
    }

    @Test
    void updateSchoolStatus_shouldThrowNotFoundWhenSchoolMissing() {
        when(schoolRepository.findById(5L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class,
                () -> service.updateSchoolStatus(5L, SchoolStatus.ACTIVE));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- getSchoolUsers ---

    @Test
    void getSchoolUsers_shouldReturnMappedUserList() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        AppUser user = new AppUser();
        user.setId(10L);
        user.setSub("sub-abc");
        user.setRole("leerkracht");
        user.setActive(true);

        when(appUserRepository.findBySchool_Id(1L)).thenReturn(List.of(user));

        List<AdminUserListItem> result = service.getSchoolUsers(1L);

        assertEquals(1, result.size());
        AdminUserListItem item = result.get(0);
        assertEquals(10L, item.getId());
        assertEquals("sub-abc", item.getSub());
        assertEquals("leerkracht", item.getRole());
        assertTrue(item.isActive());
        assertNull(item.getKlasNaam());
    }

    @Test
    void getSchoolUsers_shouldThrowNotFoundWhenSchoolMissing() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getSchoolUsers(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- toggleUserActive ---

    @Test
    void toggleUserActive_shouldFlipActiveStateFromTrueToFalse() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setSub("sub-x");
        user.setRole("leerkracht");
        user.setActive(true);
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AdminUserListItem result = service.toggleUserActive(1L, 5L);

        assertFalse(result.isActive());
    }

    @Test
    void toggleUserActive_shouldFlipActiveStateFromFalseToTrue() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setSub("sub-x");
        user.setRole("leerkracht");
        user.setActive(false);
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AdminUserListItem result = service.toggleUserActive(1L, 5L);

        assertTrue(result.isActive());
    }

    @Test
    void toggleUserActive_shouldThrowNotFoundWhenUserMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.toggleUserActive(1L, 99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("USER_NOT_FOUND", ex.getCode());
    }

    @Test
    void toggleUserActive_shouldThrowForbiddenWhenUserBelongsToDifferentSchool() {
        School otherSchool = makeSchool(99L, "other", "https://other.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setSchool(otherSchool);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.toggleUserActive(1L, 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
        assertEquals("ACCESS_DENIED", ex.getCode());
    }

    @Test
    void toggleUserActive_shouldThrowForbiddenWhenUserHasNoSchool() {
        AppUser user = new AppUser();
        user.setId(5L);
        user.setSchool(null);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.toggleUserActive(1L, 5L));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    // --- setUserRole ---

    @Test
    void setUserRole_shouldChangeLeerkrachtToBibbeheerder() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setRole("leerkracht");
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AdminUserListItem result = service.setUserRole(1L, 5L, "bibbeheerder");

        assertEquals("bibbeheerder", result.getRole());
    }

    @Test
    void setUserRole_shouldChangeBibbeheerderToLeerkracht() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setRole("bibbeheerder");
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AdminUserListItem result = service.setUserRole(1L, 5L, "leerkracht");

        assertEquals("leerkracht", result.getRole());
    }

    @Test
    void setUserRole_shouldThrowNotFoundWhenUserMissing() {
        when(appUserRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.setUserRole(1L, 99L, "leerkracht"));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    @Test
    void setUserRole_shouldThrowForbiddenWhenUserBelongsToDifferentSchool() {
        School otherSchool = makeSchool(99L, "other", "https://other.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setRole("leerkracht");
        user.setSchool(otherSchool);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.setUserRole(1L, 5L, "bibbeheerder"));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus());
    }

    @Test
    void setUserRole_shouldThrowBadRequestForInvalidRole() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setRole("leerkracht");
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.setUserRole(1L, 5L, "leerling"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_ROLE", ex.getCode());
    }

    @Test
    void setUserRole_shouldThrowBadRequestWhenUserIsLeerling() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        AppUser user = new AppUser();
        user.setId(5L);
        user.setRole("leerling");
        user.setSchool(school);

        when(appUserRepository.findById(5L)).thenReturn(Optional.of(user));

        ApiException ex = assertThrows(ApiException.class, () -> service.setUserRole(1L, 5L, "leerkracht"));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_ROLE_TRANSITION", ex.getCode());
    }

    // --- getSchoolKlassen ---

    @Test
    void getSchoolKlassen_shouldReturnMappedKlassen() {
        School school = makeSchool(1L, "school", "https://school.smartschool.be");
        when(schoolRepository.findById(1L)).thenReturn(Optional.of(school));

        Klas klas = new Klas();
        klas.setId(10L);
        klas.setGroupId("group-1");
        klas.setNaam("6A");
        klas.setSchool(school);

        when(klasRepository.findBySchool_Id(1L)).thenReturn(List.of(klas));

        List<KlasListItem> result = service.getSchoolKlassen(1L);

        assertEquals(1, result.size());
        KlasListItem item = result.get(0);
        assertEquals(10L, item.getId());
        assertEquals("group-1", item.getGroupId());
        assertEquals("6A", item.getNaam());
    }

    @Test
    void getSchoolKlassen_shouldThrowNotFoundWhenSchoolMissing() {
        when(schoolRepository.findById(99L)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.getSchoolKlassen(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
    }

    // --- helpers ---

    private School makeSchool(Long id, String subdomain, String url) {
        School school = new School();
        school.setId(id);
        school.setSubdomein(subdomain);
        school.setSmartschoolUrl(url);
        school.setStatus(SchoolStatus.PENDING);
        return school;
    }

    private void stubDetailCounts(Long schoolId) {
        when(appUserRepository.countBySchool_Id(schoolId)).thenReturn(0L);
        when(klasRepository.countBySchool_Id(schoolId)).thenReturn(0L);
        when(bookRepository.countBySchool_Id(schoolId)).thenReturn(0L);
        when(loanRepository.countByCopy_Book_School_IdAndReturnedAtIsNull(schoolId)).thenReturn(0L);
        when(wishlistRepository.countByBook_School_Id(schoolId)).thenReturn(0L);
        when(classReadingListItemRepository.countBySchoolId(schoolId)).thenReturn(0L);
    }
}
