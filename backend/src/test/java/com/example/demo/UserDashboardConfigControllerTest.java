package com.example.demo;

import com.example.demo.controllers.UserDashboardConfigController;
import com.example.demo.controllers.UserDashboardConfigController.DashboardConfigRequest;
import com.example.demo.controllers.UserDashboardConfigController.DashboardConfigResponse;
import com.example.demo.entities.UserDashboardConfig;
import com.example.demo.repositories.UserDashboardConfigRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;

import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDashboardConfigController Tests")
@SuppressWarnings("null")
class UserDashboardConfigControllerTest {

    @Mock
    private UserDashboardConfigRepository repository;

    @InjectMocks
    private UserDashboardConfigController controller;

    private static final String USER_SUB = "user-abc";
    private static final String SAMPLE_JSON =
            "{\"tiles\":[\"loans\",\"history\"],\"shortcuts\":[\"add-book\"]}";

    private Authentication authFor(String sub) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn(sub);
        return auth;
    }

    private DashboardConfigRequest requestWith(String json) {
        DashboardConfigRequest req = new DashboardConfigRequest();
        req.setConfigJson(json);
        return req;
    }

    @Test
    @DisplayName("GET should return 401 when authentication is null")
    void testGetConfigUnauthorized() {
        ResponseEntity<DashboardConfigResponse> response = controller.getConfig(null);

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verify(repository, never()).findByUserSub(any());
    }

    @Test
    @DisplayName("GET should return saved configJson for the authenticated user")
    void testGetConfigReturnsSavedJson() {
        UserDashboardConfig entity = new UserDashboardConfig(USER_SUB, SAMPLE_JSON);
        when(repository.findByUserSub(USER_SUB)).thenReturn(Optional.of(entity));

        ResponseEntity<DashboardConfigResponse> response = controller.getConfig(authFor(USER_SUB));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardConfigResponse body = Objects.requireNonNull(response.getBody());
        assertEquals(SAMPLE_JSON, body.getConfigJson());
    }

    @Test
    @DisplayName("GET should return 200 with null configJson when user has no saved config")
    void testGetConfigReturnsNullWhenAbsent() {
        when(repository.findByUserSub(USER_SUB)).thenReturn(Optional.empty());

        ResponseEntity<DashboardConfigResponse> response = controller.getConfig(authFor(USER_SUB));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        DashboardConfigResponse body = Objects.requireNonNull(response.getBody());
        assertNull(body.getConfigJson());
    }

    @Test
    @DisplayName("GET should query repository using the authenticated user's name")
    void testGetConfigUsesAuthenticationName() {
        when(repository.findByUserSub("other-user")).thenReturn(Optional.empty());

        controller.getConfig(authFor("other-user"));

        verify(repository).findByUserSub("other-user");
        verifyNoMoreInteractions(repository);
    }

    @Test
    @DisplayName("PUT should return 401 when authentication is null")
    void testSaveConfigUnauthorized() {
        ResponseEntity<Void> response = controller.saveConfig(null, requestWith(SAMPLE_JSON));

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("PUT should return 400 when request body is null")
    void testSaveConfigBadRequestNullBody() {
        ResponseEntity<Void> response = controller.saveConfig(mock(Authentication.class), null);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("PUT should return 400 when configJson is null")
    void testSaveConfigBadRequestNullJson() {
        ResponseEntity<Void> response = controller.saveConfig(mock(Authentication.class), requestWith(null));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    @DisplayName("PUT should return 413 when configJson exceeds 8KB limit")
    void testSaveConfigPayloadTooLarge() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < (8 * 1024) + 1; i++) {
            sb.append('x');
        }

        ResponseEntity<Void> response = controller.saveConfig(mock(Authentication.class), requestWith(sb.toString()));

        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE, response.getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("PUT should create a new entity when user has no existing config")
    void testSaveConfigCreatesNewEntity() {
        when(repository.findByUserSub(USER_SUB)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.saveConfig(authFor(USER_SUB), requestWith(SAMPLE_JSON));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<UserDashboardConfig> captor = ArgumentCaptor.forClass(UserDashboardConfig.class);
        verify(repository).save(captor.capture());
        UserDashboardConfig saved = captor.getValue();
        assertEquals(USER_SUB, saved.getUserSub());
        assertEquals(SAMPLE_JSON, saved.getConfigJson());
        assertNull(saved.getId(), "new entity should not have an id assigned by the controller");
    }

    @Test
    @DisplayName("PUT should update existing entity in place rather than insert a duplicate")
    void testSaveConfigUpdatesExisting() {
        UserDashboardConfig existing = new UserDashboardConfig(USER_SUB, "{\"tiles\":[\"loans\"]}");
        existing.setId(42L);
        when(repository.findByUserSub(USER_SUB)).thenReturn(Optional.of(existing));

        String updatedJson = "{\"tiles\":[\"loans\",\"history\"],\"shortcuts\":[\"add-book\"]}";
        ResponseEntity<Void> response = controller.saveConfig(authFor(USER_SUB), requestWith(updatedJson));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<UserDashboardConfig> captor = ArgumentCaptor.forClass(UserDashboardConfig.class);
        verify(repository).save(captor.capture());
        UserDashboardConfig saved = captor.getValue();
        assertEquals(42L, saved.getId());
        assertEquals(USER_SUB, saved.getUserSub());
        assertEquals(updatedJson, saved.getConfigJson());
    }

    @Test
    @DisplayName("PUT should accept empty json string (treated as cleared config, not invalid)")
    void testSaveConfigAcceptsEmptyJsonString() {
        when(repository.findByUserSub(USER_SUB)).thenReturn(Optional.empty());

        ResponseEntity<Void> response = controller.saveConfig(authFor(USER_SUB), requestWith(""));

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        ArgumentCaptor<UserDashboardConfig> captor = ArgumentCaptor.forClass(UserDashboardConfig.class);
        verify(repository).save(captor.capture());
        assertEquals("", captor.getValue().getConfigJson());
    }

    @Test
    @DisplayName("PUT should scope persistence to the authenticated user's sub")
    void testSaveConfigUsesAuthenticationName() {
        when(repository.findByUserSub("user-x")).thenReturn(Optional.empty());

        controller.saveConfig(authFor("user-x"), requestWith(SAMPLE_JSON));

        verify(repository).findByUserSub("user-x");
        ArgumentCaptor<UserDashboardConfig> captor = ArgumentCaptor.forClass(UserDashboardConfig.class);
        verify(repository).save(captor.capture());
        assertEquals("user-x", captor.getValue().getUserSub());
    }
}
