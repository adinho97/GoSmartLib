package com.example.demo.oneroster;

import com.example.demo.oneroster.dto.OneRosterSyncResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * Eager population on boot: walk every configured OneRoster school and upsert
 * its roster. Iterates the config map (not the DB) so a freshly configured
 * school with zero rows in {@code scholen} still gets created.
 *
 * Runs async on the {@code oneRosterTaskExecutor} thread pool, so a slow or
 * unreachable OneRoster endpoint cannot block Spring boot.
 */
@Component
public class OneRosterStartupSync {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterStartupSync.class);

    private final OneRosterProperties properties;
    private final OneRosterSyncService syncService;

    public OneRosterStartupSync(OneRosterProperties properties, OneRosterSyncService syncService) {
        this.properties = properties;
        this.syncService = syncService;
    }

    @Async("oneRosterTaskExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void runStartupSync() {
        Map<String, OneRosterProperties.SchoolConfig> all = properties.getSchools();
        if (all == null || all.isEmpty()) {
            logger.debug("No OneRoster schools configured, startup sync skipped");
            return;
        }

        for (Map.Entry<String, OneRosterProperties.SchoolConfig> entry : all.entrySet()) {
            String subdomain = entry.getKey();
            OneRosterProperties.SchoolConfig cfg = entry.getValue();
            if (cfg == null || !cfg.isUsable()) {
                logger.debug("OneRoster config for '{}' is missing or incomplete (likely no client_secret env var), skipping",
                        subdomain);
                continue;
            }
            try {
                OneRosterSyncResult result = syncService.syncBySubdomain(subdomain);
                if (!result.getErrors().isEmpty()) {
                    logger.warn("OneRoster startup sync for {} finished with errors: {}",
                            subdomain, result.getErrors());
                }
            } catch (Exception e) {
                logger.warn("OneRoster startup sync threw for {}: {}", subdomain, e.getMessage(), e);
            }
        }
    }
}
