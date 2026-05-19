package com.example.demo.oneroster;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.oneroster.OneRosterProperties.SchoolConfig;
import com.example.demo.oneroster.dto.OneRosterClass;
import com.example.demo.oneroster.dto.OneRosterEnrollment;
import com.example.demo.oneroster.dto.OneRosterOrg;
import com.example.demo.oneroster.dto.OneRosterSyncResult;
import com.example.demo.oneroster.dto.OneRosterUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
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
 *   - AppUser (existing): only klas may change. Never touch accessToken,
 *     smartschoolRefreshToken, active, role, platform, sub, school.
 *   - AppUser (in DB, missing from OneRoster): leave alone (no delete).
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

    // Tracks subdomains with an in-flight sync, so a cron firing while an
    // admin clicks the sync button (or two clicks land at the same time)
    // doesn't race on the (school_id, group_id) and app_users.sub unique
    // constraints. Second concurrent call returns skippedEntirely=true.
    private final Set<String> inFlightSubdomains = ConcurrentHashMap.newKeySet();

    public OneRosterSyncService(OneRosterProperties properties,
            OneRosterClient client,
            SchoolRepository schoolRepository,
            KlasRepository klasRepository,
            AppUserRepository appUserRepository) {
        this.properties = properties;
        this.client = client;
        this.schoolRepository = schoolRepository;
        this.klasRepository = klasRepository;
        this.appUserRepository = appUserRepository;
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
                    + "classesCreated={}, classesUpdated={}, usersCreated={}, usersUpdated={}, skipped={}",
                    subdomain, result.getSchoolsCreated(), result.getSchoolsUpdated(),
                    result.getClassesCreated(), result.getClassesUpdated(),
                    result.getUsersCreated(), result.getUsersUpdated(), result.getSkipped());
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
                result.incrementSchoolsUpdated();
                return saved;
            }
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
        result.incrementSchoolsCreated();
        return saved;
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
                if (!usersAlreadyAssigned.contains(row.userSub)) {
                    Klas currentKlas = existing.getKlas();
                    boolean klasChanged = currentKlas == null
                            || !Objects.equals(currentKlas.getId(), klas.getId());
                    if (klasChanged) {
                        existing.setKlas(klas);
                        appUserRepository.save(existing);
                        result.incrementUsersUpdated();
                    }
                    usersAlreadyAssigned.add(row.userSub);
                }
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
