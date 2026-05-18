package com.example.demo.services;

import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.entities.School;
import com.example.demo.oneroster.OneRosterClient;
import com.example.demo.oneroster.OneRosterProperties;
import com.example.demo.oneroster.dto.OneRosterEnrollment;
import com.example.demo.oneroster.dto.OneRosterUser;
import com.example.demo.repositories.SchoolRepository;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Resolves Smartschool sub -> display name without ever persisting names.
 *
 * AppUser intentionally has no username/displayName columns (commit 6ab0761b).
 * Names live in memory only: cached here, never written to the DB.
 *
 * Two sources, in order:
 * 1. OneRoster (preferred): batched per school using admin credentials,
 * so one call covers everyone — works even for users who never logged in.
 * 2. Smartschool userinfo (fallback): per-user, requires the target user's
 * own refresh token. Used only when OneRoster is absent or incomplete.
 */
@Service
public class DisplayNameResolver {

    private static final Logger logger = LoggerFactory.getLogger(DisplayNameResolver.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(15);
    private static final Duration ONEROSTER_REFETCH_COOLDOWN = Duration.ofMinutes(5);

    private record Entry(String displayName, long expiresAt) {
    }

    private final ConcurrentHashMap<String, Entry> nameCache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> oneRosterLastFetch = new ConcurrentHashMap<>();

    private final AuthService authService;
    private final OneRosterClient oneRosterClient;
    private final OneRosterProperties oneRosterProperties;
    private final SchoolRepository schoolRepository;

    public DisplayNameResolver(AuthService authService,
            OneRosterClient oneRosterClient,
            OneRosterProperties oneRosterProperties,
            SchoolRepository schoolRepository) {
        this.authService = authService;
        this.oneRosterClient = oneRosterClient;
        this.oneRosterProperties = oneRosterProperties;
        this.schoolRepository = schoolRepository;
    }

    /**
     * Resolve display names for the given subs. Cache hits return immediately;
     * misses trigger a OneRoster school batch (if configured) and then per-user
     * Smartschool fallback for anything still missing. Failed lookups return
     * the sub itself so callers always get a value.
     */
    public Map<String, String> resolveAll(Long schoolId, Collection<String> subs) {
        Map<String, String> result = new HashMap<>();
        if (subs == null || subs.isEmpty()) {
            return result;
        }

        long now = System.currentTimeMillis();
        Set<String> misses = new HashSet<>();
        for (String sub : subs) {
            if (sub == null || sub.isBlank()) {
                continue;
            }
            Entry cached = nameCache.get(sub);
            if (cached != null && cached.expiresAt > now) {
                result.put(sub, cached.displayName);
            } else {
                misses.add(sub);
            }
        }

        if (misses.isEmpty()) {
            return result;
        }

        String subdomain = schoolId == null ? null : subdomainForSchool(schoolId);
        if (subdomain != null && oneRosterProperties.isConfigured(subdomain)
                && shouldRefetchOneRoster(subdomain, now)) {
            refreshOneRosterCacheForSchool(subdomain, now);
            long after = System.currentTimeMillis();
            misses.removeIf(sub -> {
                Entry cached = nameCache.get(sub);
                if (cached != null && cached.expiresAt > after) {
                    result.put(sub, cached.displayName);
                    return true;
                }
                return false;
            });
        }

        if (misses.isEmpty()) {
            return result;
        }

        Map<String, String> fetched = Flux.fromIterable(misses)
                .flatMap(sub -> authService.getUserInfoBySub(sub)
                        .map(info -> Map.entry(sub, formatFromSmartschool(info, sub)))
                        .onErrorResume(err -> {
                            logger.debug("Smartschool name lookup failed for {}: {}", sub, err.getMessage());
                            return Mono.just(Map.entry(sub, sub));
                        }))
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .block();

        if (fetched != null) {
            long expiresAt = System.currentTimeMillis() + CACHE_TTL.toMillis();
            fetched.forEach((sub, name) -> {
                nameCache.put(sub, new Entry(name, expiresAt));
                result.put(sub, name);
            });
        }

        return result;
    }

    /**
     * Best-effort: if OneRoster is configured for this school and the cooldown
     * has elapsed, fetch the school's enrollments in one batch and populate
     * the cache for everyone. No-op for schools without OneRoster. Used by
     * search so name-based filtering can hit a warm cache via {@link #peek}.
     */
    public void warmSchoolCache(Long schoolId) {
        if (schoolId == null) {
            return;
        }
        String subdomain = subdomainForSchool(schoolId);
        long now = System.currentTimeMillis();
        if (subdomain != null && oneRosterProperties.isConfigured(subdomain)
                && shouldRefetchOneRoster(subdomain, now)) {
            refreshOneRosterCacheForSchool(subdomain, now);
        }
    }

    /** Cache-only lookup; never triggers a network call. */
    public Optional<String> peek(String sub) {
        if (sub == null || sub.isBlank()) {
            return Optional.empty();
        }
        Entry cached = nameCache.get(sub);
        if (cached == null || cached.expiresAt <= System.currentTimeMillis()) {
            return Optional.empty();
        }
        return Optional.of(cached.displayName);
    }

    private String subdomainForSchool(Long schoolId) {
        return schoolRepository.findById(schoolId)
                .map(School::getSubdomein)
                .orElse(null);
    }

    private boolean shouldRefetchOneRoster(String subdomain, long now) {
        Long last = oneRosterLastFetch.get(subdomain);
        return last == null || (now - last) > ONEROSTER_REFETCH_COOLDOWN.toMillis();
    }

    private void refreshOneRosterCacheForSchool(String subdomain, long now) {
        try {
            List<OneRosterEnrollment> enrollments = oneRosterClient.getEnrollmentsBySchool(subdomain).block();
            if (enrollments == null || enrollments.isEmpty()) {
                oneRosterLastFetch.put(subdomain, now);
                return;
            }
            long expiresAt = now + CACHE_TTL.toMillis();
            Set<String> seen = new HashSet<>();
            for (OneRosterEnrollment enrollment : enrollments) {
                if (enrollment == null) {
                    continue;
                }
                OneRosterUser user = enrollment.getUser();
                if (user == null) {
                    continue;
                }
                String sub = user.legacyIdentifier();
                if (sub == null || sub.isBlank() || !seen.add(sub)) {
                    continue;
                }
                String name = formatFromOneRoster(user, sub);
                nameCache.put(sub, new Entry(name, expiresAt));
            }
            oneRosterLastFetch.put(subdomain, now);
            logger.debug("OneRoster name cache populated for {} ({} users)", subdomain, seen.size());
        } catch (Exception e) {
            // Mark as recently attempted even on failure, so a down OneRoster API
            // doesn't get hammered on every request. The cooldown will expire.
            oneRosterLastFetch.put(subdomain, now);
            logger.warn("OneRoster name fetch failed for {}: {}; using per-user Smartschool fallback",
                    subdomain, e.getMessage());
        }
    }

    private String formatFromOneRoster(OneRosterUser user, String fallbackSub) {
        return formatName(user.getGivenName(), user.getFamilyName(), null, fallbackSub);
    }

    private String formatFromSmartschool(SmartschoolUserInfo info, String fallbackSub) {
        if (info == null) {
            return fallbackSub;
        }
        String fullNameFallback = (info.getFullName() != null && !info.getFullName().isBlank())
                ? info.getFullName()
                : info.getName();
        return formatName(info.getGivenName(), info.getFamilyName(), fullNameFallback, fallbackSub);
    }

    private static String formatName(String given, String family, String fullNameFallback, String sub) {
        boolean hasGiven = given != null && !given.isBlank();
        boolean hasFamily = family != null && !family.isBlank();
        if (hasFamily && hasGiven) {
            return family + " " + given;
        }
        if (hasFamily) {
            return family;
        }
        if (hasGiven) {
            return given;
        }
        if (fullNameFallback != null && !fullNameFallback.isBlank()) {
            return fullNameFallback;
        }
        return sub;
    }
}
