package com.example.demo;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.oneroster.OneRosterClient;
import com.example.demo.oneroster.OneRosterProperties;
import com.example.demo.oneroster.OneRosterSyncService;
import com.example.demo.oneroster.dto.OneRosterClass;
import com.example.demo.oneroster.dto.OneRosterEnrollment;
import com.example.demo.oneroster.dto.OneRosterMetadata;
import com.example.demo.oneroster.dto.OneRosterOrg;
import com.example.demo.oneroster.dto.OneRosterSyncResult;
import com.example.demo.oneroster.dto.OneRosterUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.repositories.SchoolSettingsRepository;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DataJpaTest
@AutoConfigureTestDatabase(replace = Replace.ANY)
@ActiveProfiles("test")
@DisplayName("OneRosterSyncService Tests")
class OneRosterSyncServiceTest {

    @Autowired
    private SchoolRepository schoolRepository;
    @Autowired
    private AppUserRepository appUserRepository;
    @Autowired
    private KlasRepository klasRepository;
    @Autowired
    private SchoolSettingsRepository schoolSettingsRepository;

    private OneRosterClient oneRosterClient;
    private OneRosterProperties properties;
    private OneRosterSyncService syncService;

    private static final String SUBDOMAIN = "aphogeschool";

    @BeforeEach
    void setUp() {
        oneRosterClient = mock(OneRosterClient.class);
        properties = new OneRosterProperties();

        OneRosterProperties.SchoolConfig cfg = new OneRosterProperties.SchoolConfig();
        cfg.setClientId("test-client-id");
        cfg.setClientSecret("test-client-secret");
        cfg.setSchoolId("5905");
        cfg.setBaseUrl("https://aphogeschool.smartschool.be/ims/oneroster");
        properties.setSchools(new HashMap<>());
        properties.getSchools().put(SUBDOMAIN, cfg);

        syncService = new OneRosterSyncService(properties, oneRosterClient,
                schoolRepository, klasRepository, appUserRepository, schoolSettingsRepository);
    }

    @Test
    @DisplayName("school safety: admin-edited fields are not overwritten")
    void schoolSafety_adminEditedFieldsPreserved() {
        School existing = new School();
        existing.setSubdomein(SUBDOMAIN);
        existing.setSmartschoolUrl("https://aphogeschool.smartschool.be");
        existing.setNaam("Custom Admin Name");
        existing.setAdres("Foo 1");
        existing.setLatitude(51.0);
        existing.setLongitude(4.5);
        existing.setStatus(SchoolStatus.ACTIVE);
        schoolRepository.save(existing);

        mockOrg("AP Hogeschool from OneRoster");
        mockEnrollments(List.of());

        syncService.syncBySubdomain(SUBDOMAIN);

        School after = schoolRepository.findBySubdomeinIgnoreCase(SUBDOMAIN).orElseThrow();
        assertEquals("Custom Admin Name", after.getNaam(),
                "admin-set naam must not be overwritten when non-blank");
        assertEquals("Foo 1", after.getAdres());
        assertEquals(51.0, after.getLatitude());
        assertEquals(4.5, after.getLongitude());
        assertEquals(SchoolStatus.ACTIVE, after.getStatus());
    }

    @Test
    @DisplayName("school safety: PENDING -> ACTIVE, naam filled when blank")
    void schoolSafety_pendingTransition() {
        School existing = new School();
        existing.setSubdomein(SUBDOMAIN);
        existing.setSmartschoolUrl("https://aphogeschool.smartschool.be");
        existing.setNaam(null);
        existing.setStatus(SchoolStatus.PENDING);
        schoolRepository.save(existing);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of());

        syncService.syncBySubdomain(SUBDOMAIN);

        School after = schoolRepository.findBySubdomeinIgnoreCase(SUBDOMAIN).orElseThrow();
        assertEquals("AP Hogeschool", after.getNaam());
        assertEquals(SchoolStatus.ACTIVE, after.getStatus());
    }

    @Test
    @DisplayName("school safety: INACTIVE is never reactivated by sync")
    void schoolSafety_inactiveProtected() {
        School existing = new School();
        existing.setSubdomein(SUBDOMAIN);
        existing.setSmartschoolUrl("https://aphogeschool.smartschool.be");
        existing.setNaam("School");
        existing.setStatus(SchoolStatus.INACTIVE);
        schoolRepository.save(existing);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of());

        syncService.syncBySubdomain(SUBDOMAIN);

        School after = schoolRepository.findBySubdomeinIgnoreCase(SUBDOMAIN).orElseThrow();
        assertEquals(SchoolStatus.INACTIVE, after.getStatus(),
                "super-admin deactivation must stick across sync runs");
    }

    @Test
    @DisplayName("school creation: missing school is inserted with subdomain/url/active status")
    void schoolCreation_freshInsert() {
        assertTrue(schoolRepository.findBySubdomeinIgnoreCase(SUBDOMAIN).isEmpty());

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of());

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(1, result.getSchoolsCreated());
        School after = schoolRepository.findBySubdomeinIgnoreCase(SUBDOMAIN).orElseThrow();
        assertEquals(SUBDOMAIN, after.getSubdomein());
        assertEquals("https://aphogeschool.smartschool.be", after.getSmartschoolUrl());
        assertEquals("AP Hogeschool", after.getNaam());
        assertEquals(SchoolStatus.ACTIVE, after.getStatus());
        assertNull(after.getAdres());
        assertNull(after.getLatitude());
        assertNull(after.getLongitude());
    }

    @Test
    @DisplayName("user safety: existing user's session/role/active fields are untouched")
    void userSafety_existingUserFieldsPreserved() {
        School school = persistedSchool();
        Klas oldKlas = persistKlas(school, "OLDKLAS", "Old Klas");

        AppUser existing = new AppUser();
        existing.setSub("USER_SUB_1");
        existing.setRole("bibbeheerder");
        existing.setAccessToken("xyz-access");
        existing.setSmartschoolRefreshToken("xyz-refresh");
        existing.setPlatform("https://aphogeschool.smartschool.be");
        existing.setActive(true);
        existing.setSchool(school);
        existing.setKlas(oldKlas);
        appUserRepository.save(existing);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "USER_SUB_1", "NEWKLAS", "New Klas")));

        syncService.syncBySubdomain(SUBDOMAIN);

        AppUser after = appUserRepository.findBySub("USER_SUB_1").orElseThrow();
        assertEquals("bibbeheerder", after.getRole(), "role must not change on existing user");
        assertEquals("xyz-access", after.getAccessToken(), "accessToken must not be touched");
        assertEquals("xyz-refresh", after.getSmartschoolRefreshToken(),
                "refresh token must not be touched");
        assertTrue(after.isActive(), "active must not be touched");
        assertEquals("https://aphogeschool.smartschool.be", after.getPlatform());
        assertNotNull(after.getKlas());
        assertEquals("NEWKLAS", after.getKlas().getGroupId(),
                "klas membership is the only field sync may change");
    }

    @Test
    @DisplayName("user safety: user not present in OneRoster keeps PII; only departure flags change")
    void userSafety_unmentionedUserPreserved() {
        School school = persistedSchool();
        Klas klas = persistKlas(school, "K1", "Klas 1");

        AppUser alumnus = new AppUser();
        alumnus.setSub("ALUMNUS_SUB");
        alumnus.setRole("leerling");
        alumnus.setAccessToken("alumnus-token");
        alumnus.setActive(true);
        alumnus.setSchool(school);
        alumnus.setKlas(klas);
        appUserRepository.save(alumnus);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "OTHER_USER", "K1", "Klas 1")));

        syncService.syncBySubdomain(SUBDOMAIN);

        AppUser after = appUserRepository.findBySub("ALUMNUS_SUB").orElseThrow();
        assertEquals("alumnus-token", after.getAccessToken(),
                "accessToken must survive departure (PII retained for retention window)");
        assertEquals("leerling", after.getRole(),
                "role must not be rewritten by sync");
        assertFalse(after.isActive(),
                "absent user is marked inactive by slice 1 departure detection");
        assertNotNull(after.getDepartedAt(),
                "absent user gets departedAt timestamp");
    }

    @Test
    @DisplayName("klas safety: klas not in OneRoster is left intact")
    void klasSafety_unmentionedKlasPreserved() {
        School school = persistedSchool();
        persistKlas(school, "LEGACY", "Legacy Klas");

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "U1", "FRESH", "Fresh Klas")));

        syncService.syncBySubdomain(SUBDOMAIN);

        assertTrue(klasRepository.findBySchool_IdAndGroupId(school.getId(), "LEGACY").isPresent(),
                "legacy klas must not be deleted by sync");
    }

    @Test
    @DisplayName("happy path: students and teachers both seeded with correct role mapping")
    void happyPath_studentsAndTeachersBothSeeded() {
        persistedSchool();
        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(
                enrollment("student", "STUDENT_SUB_1", "K1", "Klas 1"),
                enrollment("student", "STUDENT_SUB_2", "K1", "Klas 1"),
                enrollment("teacher", "TEACHER_SUB_1", "K1", "Klas 1")));

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(3, result.getUsersCreated());
        assertEquals(1, result.getClassesCreated());
        assertEquals("leerling", appUserRepository.findBySub("STUDENT_SUB_1").orElseThrow().getRole());
        assertEquals("leerling", appUserRepository.findBySub("STUDENT_SUB_2").orElseThrow().getRole());
        assertEquals("leerkracht", appUserRepository.findBySub("TEACHER_SUB_1").orElseThrow().getRole());
    }

    @Test
    @DisplayName("idempotency: running sync twice produces no duplicates")
    void idempotency_secondRunIsNoOp() {
        persistedSchool();
        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(
                enrollment("student", "U1", "K1", "Klas 1"),
                enrollment("teacher", "T1", "K1", "Klas 1")));

        syncService.syncBySubdomain(SUBDOMAIN);
        long usersAfterFirst = appUserRepository.count();
        long klassenAfterFirst = klasRepository.count();

        OneRosterSyncResult second = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(usersAfterFirst, appUserRepository.count());
        assertEquals(klassenAfterFirst, klasRepository.count());
        assertEquals(0, second.getUsersCreated());
        assertEquals(0, second.getClassesCreated());
    }

    @Test
    @DisplayName("role mapping: unknown roles are skipped, not defaulted")
    void roleMapping_unknownRoleSkipped() {
        persistedSchool();
        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(
                enrollment("administrator", "ADMIN_SUB", "K1", "Klas 1"),
                enrollment("student", "STUDENT_SUB", "K1", "Klas 1")));

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(1, result.getUsersCreated());
        assertEquals(1, result.getSkipped());
        assertTrue(appUserRepository.findBySub("ADMIN_SUB").isEmpty(),
                "unknown role must not produce an AppUser row");
        assertTrue(appUserRepository.findBySub("STUDENT_SUB").isPresent());
    }

    @Test
    @DisplayName("multi-class teacher: first enrollment wins, no duplicate user rows")
    void multiClassTeacher_firstEnrollmentWins() {
        persistedSchool();
        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(
                enrollment("teacher", "TEACHER_MULTI", "K_FIRST", "First Klas"),
                enrollment("teacher", "TEACHER_MULTI", "K_SECOND", "Second Klas"),
                enrollment("teacher", "TEACHER_MULTI", "K_THIRD", "Third Klas")));

        syncService.syncBySubdomain(SUBDOMAIN);

        AppUser teacher = appUserRepository.findBySub("TEACHER_MULTI").orElseThrow();
        assertEquals("leerkracht", teacher.getRole());
        assertNotNull(teacher.getKlas());
        assertEquals("K_FIRST", teacher.getKlas().getGroupId(),
                "first enrollment in pagination order wins (matches AuthService.upsertKlasData)");

        long count = appUserRepository.findBySubIn(List.of("TEACHER_MULTI")).size();
        assertEquals(1, count, "no duplicate user rows for multi-class teacher");
    }

    @Test
    @DisplayName("no config: sync returns skipped result without errors")
    void noConfig_skipsCleanly() {
        properties.getSchools().clear();
        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertTrue(result.isSkippedEntirely());
        assertEquals("no config", result.getSkipReason());
        assertTrue(result.getErrors().isEmpty());
    }

    @Test
    @DisplayName("OneRoster client throws: sync catches, records error, does not propagate")
    void clientThrows_isCaught() {
        persistedSchool();
        when(oneRosterClient.getOrg(eq(SUBDOMAIN)))
                .thenReturn(Mono.error(new IllegalStateException("oneroster down")));

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertFalse(result.getErrors().isEmpty());
        assertEquals(0, result.getUsersCreated());
    }

    @Test
    @DisplayName("departure detection: existing user absent from OneRoster gets departedAt + active=false")
    void departureDetection_absentUserMarkedDeparted() {
        School school = persistedSchool();
        Klas klas = persistKlas(school, "K1", "Klas 1");

        AppUser stayingStudent = new AppUser();
        stayingStudent.setSub("STAYS_SUB");
        stayingStudent.setRole("leerling");
        stayingStudent.setActive(true);
        stayingStudent.setSchool(school);
        stayingStudent.setKlas(klas);
        appUserRepository.save(stayingStudent);

        AppUser departingStudent = new AppUser();
        departingStudent.setSub("LEAVES_SUB");
        departingStudent.setRole("leerling");
        departingStudent.setActive(true);
        departingStudent.setSchool(school);
        departingStudent.setKlas(klas);
        appUserRepository.save(departingStudent);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "STAYS_SUB", "K1", "Klas 1")));

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(1, result.getUsersDeparted(),
                "exactly one user departs (the one missing from this sync)");

        AppUser leaver = appUserRepository.findBySub("LEAVES_SUB").orElseThrow();
        assertNotNull(leaver.getDepartedAt(), "departedAt must be set on absent user");
        assertFalse(leaver.isActive(), "active must be cleared on departed user");

        AppUser stayer = appUserRepository.findBySub("STAYS_SUB").orElseThrow();
        assertNull(stayer.getDepartedAt(), "present user must not get departedAt");
        assertTrue(stayer.isActive(), "present user must stay active");
    }

    @Test
    @DisplayName("departure detection: empty enrollment payload does NOT mass-depart the school")
    void departureDetection_emptyPayloadIsSafe() {
        School school = persistedSchool();
        Klas klas = persistKlas(school, "K1", "Klas 1");

        AppUser student = new AppUser();
        student.setSub("STUDENT_SUB");
        student.setRole("leerling");
        student.setActive(true);
        student.setSchool(school);
        student.setKlas(klas);
        appUserRepository.save(student);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of());

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(0, result.getUsersDeparted(),
                "flaky empty-payload response must not mass-depart the school");
        AppUser after = appUserRepository.findBySub("STUDENT_SUB").orElseThrow();
        assertNull(after.getDepartedAt());
        assertTrue(after.isActive());
    }

    @Test
    @DisplayName("departure detection: already-departed user is not re-departed (timestamp not updated)")
    void departureDetection_alreadyDepartedUserIsSkipped() {
        School school = persistedSchool();
        Klas klas = persistKlas(school, "K1", "Klas 1");

        LocalDateTime priorDeparture = LocalDateTime.now().minusDays(10);
        AppUser leftLast = new AppUser();
        leftLast.setSub("PRIOR_LEAVER");
        leftLast.setRole("leerling");
        leftLast.setActive(false);
        leftLast.setDepartedAt(priorDeparture);
        leftLast.setSchool(school);
        leftLast.setKlas(klas);
        appUserRepository.save(leftLast);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "OTHER", "K1", "Klas 1")));

        OneRosterSyncResult result = syncService.syncBySubdomain(SUBDOMAIN);

        assertEquals(0, result.getUsersDeparted(),
                "already-departed users must not be counted again");
        AppUser after = appUserRepository.findBySub("PRIOR_LEAVER").orElseThrow();
        assertEquals(priorDeparture, after.getDepartedAt(),
                "departedAt timestamp must not be overwritten on subsequent syncs");
    }

    @Test
    @DisplayName("reactivation: returning user clears departedAt, restores active, gets new klas")
    void reactivation_returningUserIsRestored() {
        School school = persistedSchool();
        Klas oldKlas = persistKlas(school, "OLD", "Old Klas");

        AppUser returnee = new AppUser();
        returnee.setSub("RETURNEE_SUB");
        returnee.setRole("leerling");
        returnee.setAccessToken("old-token");
        returnee.setSmartschoolRefreshToken("old-refresh");
        returnee.setActive(false);
        returnee.setDepartedAt(LocalDateTime.now().minusDays(30));
        returnee.setSchool(school);
        returnee.setKlas(oldKlas);
        appUserRepository.save(returnee);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "RETURNEE_SUB", "NEW", "New Klas")));

        syncService.syncBySubdomain(SUBDOMAIN);

        AppUser after = appUserRepository.findBySub("RETURNEE_SUB").orElseThrow();
        assertNull(after.getDepartedAt(), "departedAt must be cleared on reappearance");
        assertTrue(after.isActive(), "active must be restored to true");
        assertEquals("NEW", after.getKlas().getGroupId(), "klas re-assigned");
        assertEquals("old-token", after.getAccessToken(),
                "tokens must not be touched by reactivation");
        assertEquals("old-refresh", after.getSmartschoolRefreshToken());
    }

    @Test
    @DisplayName("reactivation: same-class returnee still gets departedAt cleared (reactivation flag is independent of klas change)")
    void reactivation_sameKlasStillRestoresActive() {
        School school = persistedSchool();
        Klas klas = persistKlas(school, "K1", "Klas 1");

        AppUser returnee = new AppUser();
        returnee.setSub("RETURNEE_SUB");
        returnee.setRole("leerling");
        returnee.setActive(false);
        returnee.setDepartedAt(LocalDateTime.now().minusDays(5));
        returnee.setSchool(school);
        returnee.setKlas(klas);
        appUserRepository.save(returnee);

        mockOrg("AP Hogeschool");
        mockEnrollments(List.of(enrollment("student", "RETURNEE_SUB", "K1", "Klas 1")));

        syncService.syncBySubdomain(SUBDOMAIN);

        AppUser after = appUserRepository.findBySub("RETURNEE_SUB").orElseThrow();
        assertNull(after.getDepartedAt());
        assertTrue(after.isActive());
    }

    // ---------- helpers ----------

    private School persistedSchool() {
        School school = new School();
        school.setSubdomein(SUBDOMAIN);
        school.setSmartschoolUrl("https://aphogeschool.smartschool.be");
        school.setNaam("AP Hogeschool");
        school.setStatus(SchoolStatus.ACTIVE);
        return schoolRepository.save(school);
    }

    private Klas persistKlas(School school, String groupId, String naam) {
        Klas klas = new Klas();
        klas.setSchool(school);
        klas.setGroupId(groupId);
        klas.setNaam(naam);
        return klasRepository.save(klas);
    }

    private void mockOrg(String name) {
        OneRosterOrg org = new OneRosterOrg();
        org.setSourcedId("5905");
        org.setName(name);
        org.setStatus("active");
        org.setType("school");
        when(oneRosterClient.getOrg(eq(SUBDOMAIN))).thenReturn(Mono.just(org));
    }

    private void mockEnrollments(List<OneRosterEnrollment> enrollments) {
        when(oneRosterClient.getEnrollmentsBySchool(eq(SUBDOMAIN)))
                .thenReturn(Mono.just(new ArrayList<>(enrollments)));
    }

    private OneRosterEnrollment enrollment(String role, String userLegacyId,
            String klasGroupId, String klasTitle) {
        OneRosterEnrollment e = new OneRosterEnrollment();
        e.setRole(role);
        e.setStatus("active");

        OneRosterUser user = new OneRosterUser();
        user.setSourcedId("u-" + userLegacyId);
        OneRosterMetadata userMeta = new OneRosterMetadata();
        userMeta.setLegacyIdentifier(userLegacyId);
        user.setMetadata(userMeta);
        e.setUser(user);

        OneRosterClass clazz = new OneRosterClass();
        clazz.setSourcedId("c-" + klasGroupId);
        clazz.setTitle(klasTitle);
        OneRosterMetadata klasMeta = new OneRosterMetadata();
        klasMeta.setLegacyIdentifier(klasGroupId);
        clazz.setMetadata(klasMeta);
        e.setClazz(clazz);

        return e;
    }
}
