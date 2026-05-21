package com.example.demo.oneroster;

import com.example.demo.oneroster.dto.OneRosterSyncResult;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Automated OneRoster sync triggers:
 * - on Spring boot ({@code ApplicationReadyEvent}) so a freshly configured
 * school with zero rows in {@code scholen} gets created/populated;
 * - on 31 August 03:00 Europe/Brussels to pick up the new school year
 * (class moves + new students) before users start using the app.
 *
 * Both triggers iterate the config map (not the DB) so brand-new schools are
 * covered too. They run async on the {@code oneRosterTaskExecutor} thread
 * pool, so a slow or unreachable OneRoster endpoint cannot block boot or the
 * scheduler.
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
        syncAllConfiguredSchools("startup");
    }

    @Async("oneRosterTaskExecutor")
    @Scheduled(cron = "0 0 3 31 8 ?", zone = "Europe/Brussels")
    public void runEndOfAugustSync() {
        syncAllConfiguredSchools("end-of-august");
    }

    private void syncAllConfiguredSchools(String triggerLabel) {
        Map<String, OneRosterProperties.SchoolConfig> all = properties.getSchools();
        if (all == null || all.isEmpty()) {
            logger.debug("No OneRoster schools configured, {} sync skipped", triggerLabel);
            return;
        }

        for (Map.Entry<String, OneRosterProperties.SchoolConfig> entry : all.entrySet()) {
            String subdomain = entry.getKey();
            OneRosterProperties.SchoolConfig cfg = entry.getValue();
            if (cfg == null || !cfg.isUsable()) {
                logger.debug("OneRoster config for '{}' is missing or incomplete, skipping startup sync.", subdomain);
                continue;
            }
            syncService.syncBySubdomain(subdomain);
        }
    }
}