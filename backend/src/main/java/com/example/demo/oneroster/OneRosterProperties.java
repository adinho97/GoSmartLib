package com.example.demo.oneroster;

import java.util.HashMap;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oneroster")
public class OneRosterProperties {

    private Map<String, SchoolConfig> schools = new HashMap<>();

    public Map<String, SchoolConfig> getSchools() {
        return schools;
    }

    public void setSchools(Map<String, SchoolConfig> schools) {
        this.schools = schools;
    }

    public boolean isConfigured(String subdomain) {
        if (subdomain == null) {
            return false;
        }
        SchoolConfig cfg = schools.get(subdomain.toLowerCase());
        return cfg != null && cfg.isUsable();
    }

    public SchoolConfig get(String subdomain) {
        if (subdomain == null) {
            return null;
        }
        return schools.get(subdomain.toLowerCase());
    }

    public static class SchoolConfig {
        private String clientId;
        private String clientSecret;
        private String schoolId;
        private String baseUrl;

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getSchoolId() {
            return schoolId;
        }

        public void setSchoolId(String schoolId) {
            this.schoolId = schoolId;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        /**
         * A config entry is "usable" only if all four fields are non-blank AND
         * the secret has actually been resolved (i.e. it isn't still the literal
         * "${ONEROSTER_..._CLIENT_SECRET}" placeholder from application.properties).
         * This lets dev environments boot without OneRoster credentials.
         */
        public boolean isUsable() {
            return notBlank(clientId)
                    && notBlank(clientSecret)
                    && !clientSecret.startsWith("${")
                    && notBlank(schoolId)
                    && notBlank(baseUrl);
        }

        private static boolean notBlank(String s) {
            return s != null && !s.isBlank();
        }
    }
}
