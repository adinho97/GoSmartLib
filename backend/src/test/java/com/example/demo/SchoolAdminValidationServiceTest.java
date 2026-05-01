package com.example.demo;

import com.example.demo.exception.ApiException;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.repositories.SchoolRepository;
import com.example.demo.services.SchoolAdminValidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchoolAdminValidationServiceTest {

    @Mock
    private SchoolRepository schoolRepository;

    private SchoolAdminValidationService service;

    @BeforeEach
    void setUp() {
        service = new SchoolAdminValidationService(schoolRepository);
    }

    // --- normalizeSubdomain ---

    @Test
    void normalizeSubdomain_shouldLowercaseAndTrim() {
        String result = service.normalizeSubdomain("  MySchool  ");
        assertEquals("myschool", result);
    }

    @Test
    void normalizeSubdomain_shouldAcceptValidSubdomain() {
        String result = service.normalizeSubdomain("go-atheneum-antwerpen");
        assertEquals("go-atheneum-antwerpen", result);
    }

    @Test
    void normalizeSubdomain_shouldAcceptAlphanumericWithDashes() {
        String result = service.normalizeSubdomain("school123");
        assertEquals("school123", result);
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenNull() {
        ApiException ex = assertThrows(ApiException.class, () -> service.normalizeSubdomain(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenBlankAfterTrim() {
        ApiException ex = assertThrows(ApiException.class, () -> service.normalizeSubdomain("   "));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenHttpUrlProvided() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.normalizeSubdomain("http://myschool.smartschool.be"));
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenHttpsUrlProvided() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.normalizeSubdomain("https://myschool.smartschool.be"));
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenFullSmartschoolDomainProvided() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.normalizeSubdomain("myschool.smartschool.be"));
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenContainsSlash() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.normalizeSubdomain("myschool/path"));
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    @Test
    void normalizeSubdomain_shouldThrowWhenContainsInvalidCharacters() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.normalizeSubdomain("my school!"));
        assertEquals("INVALID_SUBDOMAIN", ex.getCode());
    }

    // --- assertSubdomainAvailable ---

    @Test
    void assertSubdomainAvailable_shouldPassWhenSubdomainIsNew() {
        when(schoolRepository.existsBySubdomeinIgnoreCase("newschool")).thenReturn(false);
        assertDoesNotThrow(() -> service.assertSubdomainAvailable("newschool"));
    }

    @Test
    void assertSubdomainAvailable_shouldThrowWhenSubdomainAlreadyExists() {
        when(schoolRepository.existsBySubdomeinIgnoreCase("taken")).thenReturn(true);
        ApiException ex = assertThrows(ApiException.class, () -> service.assertSubdomainAvailable("taken"));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("SCHOOL_ALREADY_EXISTS", ex.getCode());
    }

    // --- buildSmartschoolUrl ---

    @Test
    void buildSmartschoolUrl_shouldReturnCorrectUrl() {
        String url = service.buildSmartschoolUrl("myschool");
        assertEquals("https://myschool.smartschool.be", url);
    }

    // --- validateUpdatableStatus ---

    @Test
    void validateUpdatableStatus_shouldPassForActive() {
        assertDoesNotThrow(() -> service.validateUpdatableStatus(SchoolStatus.ACTIVE));
    }

    @Test
    void validateUpdatableStatus_shouldPassForInactive() {
        assertDoesNotThrow(() -> service.validateUpdatableStatus(SchoolStatus.INACTIVE));
    }

    @Test
    void validateUpdatableStatus_shouldThrowForNull() {
        ApiException ex = assertThrows(ApiException.class, () -> service.validateUpdatableStatus(null));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_STATUS", ex.getCode());
    }

    @Test
    void validateUpdatableStatus_shouldThrowForPending() {
        ApiException ex = assertThrows(ApiException.class,
                () -> service.validateUpdatableStatus(SchoolStatus.PENDING));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("INVALID_STATUS", ex.getCode());
    }
}
