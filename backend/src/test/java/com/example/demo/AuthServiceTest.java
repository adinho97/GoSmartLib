package com.example.demo;

import com.example.demo.config.AuthLoginResponse;
import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.lang.reflect.Method;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private AppUserRepository appUserRepository;

    private SmartschoolProperties smartschoolProperties;

    private ObjectMapper objectMapper;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        smartschoolProperties = new SmartschoolProperties();
        objectMapper = new ObjectMapper();
        authService = new AuthService(webClientBuilder, smartschoolProperties, appUserRepository, objectMapper);
    }

    @Test
    void saveUserAndBuildResponse_shouldKeepExistingBibbeheerderRole() throws Exception {
        String sub = "teacher-sub";

        AppUser existing = new AppUser();
        existing.setSub(sub);
        existing.setRole("bibbeheerder");

        SmartschoolUserInfo userInfo = buildUserInfo(sub, "leerkracht", "new-access-token", "new-refresh-token",
                "https://aphogeschool.smartschool.be");

        when(appUserRepository.findBySub(sub)).thenReturn(Optional.of(existing));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("bibbeheerder", response.getRole());
        assertEquals(sub, response.getSub());
        assertEquals("new-access-token", response.getAccessToken());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("bibbeheerder", saved.getRole());
        assertEquals("new-access-token", saved.getAccessToken());
        assertEquals("new-refresh-token", saved.getSmartschoolRefreshToken());
        assertEquals("https://aphogeschool.smartschool.be", saved.getPlatform());
    }

    @Test
    void saveUserAndBuildResponse_shouldUseSmartschoolRoleForNewUser() throws Exception {
        String sub = "new-sub";

        SmartschoolUserInfo userInfo = buildUserInfo(sub, "leerkracht", "access-token", "refresh-token",
                "https://aphogeschool.smartschool.be");

        when(appUserRepository.findBySub(sub)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("leerkracht", response.getRole());
        assertEquals(sub, response.getSub());
        assertNotNull(response.getAccessToken());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("leerkracht", saved.getRole());
        assertEquals(sub, saved.getSub());
    }

    @Test
    void saveUserAndBuildResponse_shouldFallbackToLeerlingWhenSmartschoolRoleMissing() throws Exception {
        String sub = "no-role-sub";

        SmartschoolUserInfo userInfo = buildUserInfo(sub, null, "access-token", "refresh-token",
                "https://aphogeschool.smartschool.be");

        when(appUserRepository.findBySub(sub)).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("leerling", response.getRole());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(captor.capture());
        AppUser saved = captor.getValue();
        assertEquals("leerling", saved.getRole());
    }

    private SmartschoolUserInfo buildUserInfo(String sub, String role, String accessToken, String refreshToken,
            String platform) {
        SmartschoolUserInfo userInfo = new SmartschoolUserInfo();
        userInfo.setSub(sub);
        userInfo.setRole(role);
        userInfo.setName("Test User");
        userInfo.setGivenName("Test");
        userInfo.setFamilyName("User");
        userInfo.setAccessToken(accessToken);
        userInfo.setRefreshToken(refreshToken);
        userInfo.setPlatform(platform);
        return userInfo;
    }

    private AuthLoginResponse invokeSaveUserAndBuildResponse(SmartschoolUserInfo userInfo) throws Exception {
        Method method = AuthService.class.getDeclaredMethod("saveUserAndBuildResponse", SmartschoolUserInfo.class);
        method.setAccessible(true);
        return (AuthLoginResponse) method.invoke(authService, userInfo);
    }
}
