package com.example.demo.services;

import com.example.demo.entities.SchoolStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.SchoolRepository;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class SchoolAdminValidationService {

    private static final String SUBDOMAIN_PATTERN = "^[a-z0-9-]+$";
    private final SchoolRepository schoolRepository;

    public SchoolAdminValidationService(SchoolRepository schoolRepository) {
        this.schoolRepository = schoolRepository;
    }

    public String normalizeSubdomain(String rawSubdomain) {
        if (rawSubdomain == null) {
            throw new ApiException("Subdomein is verplicht", HttpStatus.BAD_REQUEST, "INVALID_SUBDOMAIN");
        }

        String normalized = rawSubdomain.trim().toLowerCase(Locale.ROOT);
        if (normalized.isEmpty()) {
            throw new ApiException("Subdomein is verplicht", HttpStatus.BAD_REQUEST, "INVALID_SUBDOMAIN");
        }

        if (normalized.startsWith("http://") || normalized.startsWith("https://")
                || normalized.contains(".smartschool.be") || normalized.contains("/")) {
            throw new ApiException("Geef enkel het subdomein op, zonder URL of domein",
                    HttpStatus.BAD_REQUEST, "INVALID_SUBDOMAIN");
        }

        if (!normalized.matches(SUBDOMAIN_PATTERN)) {
            throw new ApiException("Subdomein mag enkel a-z, 0-9 en - bevatten",
                    HttpStatus.BAD_REQUEST, "INVALID_SUBDOMAIN");
        }

        return normalized;
    }

    public void assertSubdomainAvailable(String normalizedSubdomain) {
        if (schoolRepository.existsBySubdomeinIgnoreCase(normalizedSubdomain)) {
            throw new ApiException("Dit subdomein is al geregistreerd", HttpStatus.CONFLICT, "SCHOOL_ALREADY_EXISTS");
        }
    }

    public String buildSmartschoolUrl(String normalizedSubdomain) {
        return "https://" + normalizedSubdomain + ".smartschool.be";
    }

    public void validateUpdatableStatus(SchoolStatus status) {
        if (status == null) {
            throw new ApiException("Status is verplicht", HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }
        if (status == SchoolStatus.PENDING) {
            throw new ApiException("Status PENDING kan niet manueel ingesteld worden",
                    HttpStatus.BAD_REQUEST, "INVALID_STATUS");
        }
    }
}
