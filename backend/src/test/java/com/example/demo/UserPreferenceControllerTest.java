package com.example.demo;

import com.example.demo.controllers.UserPreferenceController;
import com.example.demo.entities.UserPreference;
import com.example.demo.repositories.UserPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserPreferenceController Tests")
class UserPreferenceControllerTest {

    @Mock
    private UserPreferenceRepository userPreferenceRepository;

    @InjectMocks
    private UserPreferenceController userPreferenceController;

    private UserPreference pref1, pref2;

    @BeforeEach
    void setUp() {
        pref1 = new UserPreference("user123", "recommendationExcludeRead_trending", true);
        pref1.setId(1L);
        pref2 = new UserPreference("user123", "recommendationExcludeRead_genre", false);
        pref2.setId(2L);
    }

    @Test
    @DisplayName("should return 401 when X-User-Sub header is missing in GET")
    void testGetPreferencesUnauthorized() {
        ResponseEntity<Map<String, Boolean>> response = userPreferenceController.getUserPreferences(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(userPreferenceRepository, never()).findByUserSub(any());
    }

    @Test
    @DisplayName("should return 401 when X-User-Sub is empty in GET")
    void testGetPreferencesUnauthorizedEmpty() {
        ResponseEntity<Map<String, Boolean>> response = userPreferenceController.getUserPreferences("");

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
    }

    @Test
    @DisplayName("should return all preferences for user in map format")
    void testGetPreferencesSuccess() {
        when(userPreferenceRepository.findByUserSub("user123"))
            .thenReturn(List.of(pref1, pref2));

        ResponseEntity<Map<String, Boolean>> response = userPreferenceController.getUserPreferences("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Boolean> body = Objects.requireNonNull(response.getBody());
        assertEquals(2, body.size());
        assertTrue(body.get("recommendationExcludeRead_trending"));
        assertFalse(body.get("recommendationExcludeRead_genre"));
    }

    @Test
    @DisplayName("should return empty map when user has no preferences")
    void testGetPreferencesEmpty() {
        when(userPreferenceRepository.findByUserSub("user123"))
            .thenReturn(new ArrayList<>());

        ResponseEntity<Map<String, Boolean>> response = userPreferenceController.getUserPreferences("user123");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Boolean> body = Objects.requireNonNull(response.getBody());
        assertTrue(body.isEmpty());
    }

    @Test
    @DisplayName("should return 401 when X-User-Sub header is missing in PATCH")
    void testSavePreferenceUnauthorized() {
        UserPreferenceController.PreferenceRequest request = new UserPreferenceController.PreferenceRequest();
        request.setKey("testKey");
        request.setValue(true);

        ResponseEntity<Void> response = userPreferenceController.savePreference(null, request);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(userPreferenceRepository);
    }

    @Test
    @DisplayName("should return 400 when request key is null")
    void testSavePreferenceBadRequestNullKey() {
        UserPreferenceController.PreferenceRequest request = new UserPreferenceController.PreferenceRequest();
        request.setKey(null);
        request.setValue(true);

        ResponseEntity<Void> response = userPreferenceController.savePreference("user123", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(userPreferenceRepository);
    }

    @Test
    @DisplayName("should return 400 when request value is null")
    void testSavePreferenceBadRequestNullValue() {
        UserPreferenceController.PreferenceRequest request = new UserPreferenceController.PreferenceRequest();
        request.setKey("testKey");
        request.setValue(null);

        ResponseEntity<Void> response = userPreferenceController.savePreference("user123", request);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(userPreferenceRepository);
    }

    @Test
    @DisplayName("should create new preference when it does not exist")
    void testSavePreferenceNewPreference() {
        UserPreferenceController.PreferenceRequest request = new UserPreferenceController.PreferenceRequest();
        request.setKey("newKey");
        request.setValue(true);

        when(userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "newKey"))
            .thenReturn(Optional.empty());

        ResponseEntity<Void> response = userPreferenceController.savePreference("user123", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).save(captor.capture());
        UserPreference saved = captor.getValue();
        assertEquals("user123", saved.getUserSub());
        assertEquals("newKey", saved.getPreferenceKey());
        assertTrue(saved.getPreferenceValue());
    }

    @Test
    @DisplayName("should update existing preference")
    void testSavePreferenceUpdateExisting() {
        UserPreferenceController.PreferenceRequest request = new UserPreferenceController.PreferenceRequest();
        request.setKey("existingKey");
        request.setValue(false);

        UserPreference existing = new UserPreference("user123", "existingKey", true);
        existing.setId(1L);

        when(userPreferenceRepository.findByUserSubAndPreferenceKey("user123", "existingKey"))
            .thenReturn(Optional.of(existing));

        ResponseEntity<Void> response = userPreferenceController.savePreference("user123", request);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<UserPreference> captor = ArgumentCaptor.forClass(UserPreference.class);
        verify(userPreferenceRepository).save(captor.capture());
        UserPreference saved = captor.getValue();
        assertEquals("user123", saved.getUserSub());
        assertEquals("existingKey", saved.getPreferenceKey());
        assertFalse(saved.getPreferenceValue());
    }
}
