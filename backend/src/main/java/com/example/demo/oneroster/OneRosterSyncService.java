package com.example.demo.oneroster;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.entities.SchoolSettings;
import com.example.demo.oneroster.OneRosterProperties.SchoolConfig;
import com.example.demo.oneroster.dto.OneRosterClass;
import com.example.demo.oneroster.dto.OneRosterEnrollment;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Upsert-only OneRoster -> local DB synchronization.
 *
 * Safety rules (do not relax without re-reading the plan):
 *   - School: never overwrite admin-curated fields (naam if non-blank, adres,
 *     latitude, longitude). Only flip PENDING -> ACTIVE; never touch INACTIVE.
 *   - AppUser (existing): klas may change. departedAt is cleared and active
 *     restored to true if the user reappears in a sync (in-window reactivation
 *     before the retention purge anonymizes their sub). Never touch
 *     accessToken, smartschoolRefreshToken, role, platform, sub, school.
 *   - AppUser (in DB, NOT in this sync's enrollments): students/teachers get
 *     departedAt=now and active=false. Skipped entirely if seenSubs is empty
 *     (defensive against a flaky OneRoster response returning zero enrollments).
 *   - Klas (in DB, missing from OneRoster): leave alone (no delete).
 *   - Role mapping: student -> leerling, teacher -> leerkracht, anything else
 *     is logged and skipped (no defaulting).
 */
@Service
public class OneRosterSyncService {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterSyncService.class);

    private final OneRosterProperties properties;
    private final OneRosterClient client;
    private final SchoolRepository schoolRepository;
    private final KlasRepository klasRepository;
    private final AppUserRepository appUserRepository;
    private final SchoolSettingsRepository schoolSettingsRepository;

    // Tracks subdomains with an in-flight sync, so a cron firing while an
    // admin clicks the sync button (or two clicks land at the same time)
    // doesn't race on the (school_id, group_id) and app_users.sub unique
    // constraints. Second concurrent call returns skippedEntirely=true.
    private final Set<String> inFlightSubdomains = ConcurrentHashMap.newKeySet();

    public OneRosterSyncService(OneRosterProperties properties,
            OneRosterClient client,
            SchoolRepository schoolRepository,
            KlasRepository klasRepository,
            AppUserRepository appUserRepository,
            SchoolSettingsRepository schoolSettingsRepository) {
        this.properties = properties;
        this.client = client;
        this.schoolRepository = schoolRepository;
        this.klasRepository = klasRepository;
        this.appUserRepository = appUserRepository;
        this.schoolSettingsRepository = schoolSettingsRepository;
    }

    @Transactional
    public OneRosterSyncResult syncBySubdomain(String subdomain) {
        OneRosterSyncResult result = new OneRosterSyncResult();
        result.setSubdomain(subdomain);

        if (subdomain == null || subdomain.isBlank()) {
            result.setSkippedEntirely(true);
            result.setSkipReason("subdomain blank");
            return result;
        }

        SchoolConfig cfg = properties.get(subdomain);
        if (cfg == null || !cfg.isUsable()) {
            logger.debug("No usable OneRoster config for subdomain {}, skipping", subdomain);
            result.setSkippedEntirely(true);
            result.setSkipReason("no config");
            return result;
        }

        String guardKey = subdomain.toLowerCase(Locale.ROOT);
        if (!inFlightSubdomains.add(guardKey)) {
            logger.info("OneRoster sync for {} skipped: another sync is already in progress", subdomain);
            result.setSkippedEntirely(true);
            result.setSkipReason("sync already in progress");
            return result;
        }

        try {
            OneRosterOrg org = client.getOrg(subdomain).block();
            School school = upsertSchool(subdomain, org, result);

            List<OneRosterEnrollment> enrollments = client.getEnrollmentsBySchool(subdomain).block();
            if (enrollments == null) {
                enrollments = List.of();
            }

            applyEnrollments(school, enrollments, result);
            logger.info("OneRoster sync complete for {}: schoolsCreated={}, schoolsUpdated={}, "
                    + "classesCreated={}, classesUpdated={}, usersCreated={}, usersUpdated={}, "
                    + "usersDeparted={}, skipped={}",
                    subdomain, result.getSchoolsCreated(), result.getSchoolsUpdated(),
                    result.getClassesCreated(), result.getClassesUpdated(),
                    result.getUsersCreated(), result.getUsersUpdated(),
                    result.getUsersDeparted(), result.getSkipped());
        } catch (Exception e) {
            logger.warn("OneRoster sync failed for subdomain {}: {}", subdomain, e.getMessage(), e);
            result.addError(e.getClass().getSimpleName() + ": " + e.getMessage());
        } finally {
            inFlightSubdomains.remove(guardKey);
        }

        return result;
    }

    private School upsertSchool(String subdomain, OneRosterOrg org, OneRosterSyncResult result) {
        String normalizedSubdomain = subdomain.toLowerCase(Locale.ROOT);
        Optional<School> existingOpt = schoolRepository.findBySubdomeinIgnoreCase(normalizedSubdomain);

        if (existingOpt.isPresent()) {
            School existing = existingOpt.get();
            boolean changed = false;

            // Only fill naam if currently null/blank. Never overwrite an admin-edited name.
            if ((existing.getNaam() == null || existing.getNaam().isBlank())
                    && org != null && org.getName() != null && !org.getName().isBlank()) {
                existing.setNaam(org.getName());
                changed = true;
            }

            // PENDING -> ACTIVE mirrors AuthService login behaviour.
            // Never modify ACTIVE -> anything, never touch INACTIVE.
            if (existing.getStatus() == SchoolStatus.PENDING) {
                existing.setStatus(SchoolStatus.ACTIVE);
                changed = true;
            }

            if (changed) {
                School saved = schoolRepository.save(existing);
                ensureSettingsExist(saved);
                result.incrementSchoolsUpdated();
                return saved;
            }
            ensureSettingsExist(existing);
            return existing;
        }

        // Missing School -> create. naam falls back to subdomain when org missing/blank.
        School created = new School();
        created.setSubdomein(normalizedSubdomain);
        created.setSmartschoolUrl("https://" + normalizedSubdomain + ".smartschool.be");
        created.setNaam(org != null && org.getName() != null && !org.getName().isBlank()
                ? org.getName()
                : normalizedSubdomain);
        created.setStatus(SchoolStatus.ACTIVE);
        School saved = schoolRepository.save(created);
        ensureSettingsExist(saved);
        result.incrementSchoolsCreated();
        return saved;
    }

    /**
     * Ensures that a school has a corresponding settings record.
     * This prevents 404 errors or empty states when the librarian first
     * visits the settings page for a newly synced school.
     */
    private void ensureSettingsExist(School school) {
        if (!schoolSettingsRepository.existsById(school.getId())) {
            logger.info("Initializing default school_settings for: {}", school.getSubdomein());
            SchoolSettings settings = new SchoolSettings();
            settings.setSchool(school);
            settings.setSchoolId(school.getId());
            // The SchoolSettings entity should handle default JSON for levels/hours 
            // via column definitions or a @PrePersist hook.
            schoolSettingsRepository.save(settings);
        }
    }

    private void applyEnrollments(School school, List<OneRosterEnrollment> enrollments,
            OneRosterSyncResult result) {

        // Pre-load existing klassen and users for this school to avoid N+1 queries.
        Map<String, Klas> klasByGroupId = new HashMap<>();
        klasRepository.findBySchool_Id(school.getId())
                .forEach(k -> klasByGroupId.put(k.getGroupId(), k));

        // Gather all subs that appear in this payload, then batch-load existing users.
        Set<String> seenSubs = new HashSet<>();
        List<EnrollmentRow> rows = new ArrayList<>();
        for (OneRosterEnrollment e : enrollments) {
            EnrollmentRow row = normalize(e);
            if (row == null) {
                result.incrementSkipped();
                continue;
            }
            seenSubs.add(row.userSub);
            rows.add(row);
        }

        Map<String, AppUser> userBySub = new HashMap<>();
        if (!seenSubs.isEmpty()) {
            appUserRepository.findBySubIn(new ArrayList<>(seenSubs))
                    .forEach(u -> userBySub.put(u.getSub(), u));
        }

        Set<String> usersAlreadyAssigned = new HashSet<>();

        for (EnrollmentRow row : rows) {
            Klas klas = klasByGroupId.get(row.klasGroupId);
            if (klas == null) {
                Klas created = new Klas();
                created.setSchool(school);
                created.setGroupId(row.klasGroupId);
                created.setNaam(row.klasName);
                klas = klasRepository.save(created);
                klasByGroupId.put(row.klasGroupId, klas);
                result.incrementClassesCreated();
            } else {
                if (row.klasName != null && !row.klasName.isBlank()
                        && !Objects.equals(row.klasName, klas.getNaam())) {
                    klas.setNaam(row.klasName);
                    klas = klasRepository.save(klas);
                    result.incrementClassesUpdated();
                }
            }

            AppUser existing = userBySub.get(row.userSub);
            if (existing == null) {
                AppUser created = new AppUser();
                created.setSub(row.userSub);
                created.setRole(row.role);
                created.setSchool(school);
                created.setKlas(klas);
                created.setActive(true);
                AppUser saved = appUserRepository.save(created);
                userBySub.put(row.userSub, saved);
                usersAlreadyAssigned.add(row.userSub);
                result.incrementUsersCreated();
            } else {
                // Existing user: only klas may change, and only the first
                // enrollment in pagination order wins (matches AuthService.upsertKlasData).
                // In-window reactivation: if previously departed (before retention
                // purge anonymized the sub), clear departure and restore active.
                boolean reactivated = false;
                if (existing.getDepartedAt() != null) {
                    existing.setDepartedAt(null);
                    existing.setActive(true);
                    reactivated = true;
                }

                boolean changed = reactivated;
                if (!usersAlreadyAssigned.contains(row.userSub)) {
                    Klas currentKlas = existing.getKlas();
                    boolean klasChanged = currentKlas == null
                            || !Objects.equals(currentKlas.getId(), klas.getId());
                    if (klasChanged) {
                        existing.setKlas(klas);
                        changed = true;
                    }
                    usersAlreadyAssigned.add(row.userSub);
                }

                if (changed) {
                    appUserRepository.save(existing);
                    result.incrementUsersUpdated();
                }
            }
        }

        // Departure detection. A user in this school with departedAt=null whose
        // sub is not in this sync's seenSubs has left (or graduated). Guarded by
        // !seenSubs.isEmpty() so a flaky empty-enrollment response cannot
        // mass-depart the whole school in one run.
        if (!seenSubs.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<AppUser> schoolUsers = appUserRepository.findBySchool_Id(school.getId());
            for (AppUser user : schoolUsers) {
                if (user.getDepartedAt() != null) {
                    continue;
                }
                String userSub = user.getSub();
                if (userSub == null || seenSubs.contains(userSub)) {
                    continue;
                }
                user.setDepartedAt(now);
                user.setActive(false);
                appUserRepository.save(user);
                result.incrementUsersDeparted();
            }
        }
    }

    private EnrollmentRow normalize(OneRosterEnrollment enrollment) {
        if (enrollment == null) {
            return null;
        }
        OneRosterUser user = enrollment.getUser();
        OneRosterClass clazz = enrollment.getClazz();
        if (user == null || clazz == null) {
            return null;
        }
        String userSub = user.legacyIdentifier();
        String klasGroupId = clazz.legacyIdentifier();
        if (userSub == null || userSub.isBlank() || klasGroupId == null || klasGroupId.isBlank()) {
            return null;
        }
        String mappedRole = mapRole(enrollment.getRole());
        if (mappedRole == null) {
            logger.warn("Skipping OneRoster enrollment with unknown role: {} (user sub={})",
                    enrollment.getRole(), userSub);
            return null;
        }
        String klasName = clazz.getTitle();
        if (klasName == null || klasName.isBlank()) {
            klasName = klasGroupId;
        }
        return new EnrollmentRow(userSub.trim(), klasGroupId.trim(), klasName.trim(), mappedRole);
    }

    private static String mapRole(String oneRosterRole) {
        if (oneRosterRole == null) {
            return null;
        }
        return switch (oneRosterRole.toLowerCase(Locale.ROOT)) {
            case "student" -> "leerling";
            case "teacher" -> "leerkracht";
            default -> null;
        };
    }

    private record EnrollmentRow(String userSub, String klasGroupId, String klasName, String role) {
    }
}
