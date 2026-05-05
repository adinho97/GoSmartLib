package com.example.demo;

import com.example.demo.config.AuthLoginResponse;
import com.example.demo.config.AuthService;
import com.example.demo.config.SmartschoolProperties;
import com.example.demo.config.SmartschoolUserInfo;
import com.example.demo.entities.AppUser;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec<?> requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec<?> requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private SchoolRepository schoolRepository;

    @Mock
    private KlasRepository klasRepository;

    private SmartschoolProperties smartschoolProperties;

    private ObjectMapper objectMapper;

    private AuthService authService;

    private School aphSchool;

    @BeforeEach
    void setUp() {
        when(webClientBuilder.build()).thenReturn(webClient);
        smartschoolProperties = new SmartschoolProperties();
        objectMapper = new ObjectMapper();

        aphSchool = new School();
        aphSchool.setId(1L);
        aphSchool.setNaam("AP Hogeschool");
        aphSchool.setSubdomein("aphogeschool");
        aphSchool.setSmartschoolUrl("https://aphogeschool.smartschool.be");
        aphSchool.setStatus(SchoolStatus.ACTIVE);

        when(schoolRepository.findBySubdomeinIgnoreCase("aphogeschool")).thenReturn(Optional.of(aphSchool));

        // The login flow tries to fetch groupinfo; we don't want these tests to depend on WebClient fluent mocks.
        // Force an error so AuthService uses its onErrorResume fallback (empty group list).
        doReturn(requestHeadersUriSpec).when(webClient).get();
        doReturn(requestHeadersSpec).when(requestHeadersUriSpec)
            .uri(org.mockito.ArgumentMatchers.anyString());
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(String.class))
                .thenReturn(Mono.error(new RuntimeException("skip groupinfo in unit test")));

        authService = new AuthService(webClientBuilder, smartschoolProperties, appUserRepository, schoolRepository,
                klasRepository, objectMapper);
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
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("bibbeheerder", response.getRole());
        assertEquals(sub, response.getSub());
        assertEquals("new-access-token", response.getAccessToken());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(Objects.requireNonNull(captor.capture()));
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
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("leerkracht", response.getRole());
        assertEquals(sub, response.getSub());
        assertNotNull(response.getAccessToken());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(Objects.requireNonNull(captor.capture()));
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
        when(appUserRepository.save(any(AppUser.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AuthLoginResponse response = invokeSaveUserAndBuildResponse(userInfo);

        assertEquals("leerling", response.getRole());

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository, times(1)).save(Objects.requireNonNull(captor.capture()));
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
        @SuppressWarnings("unchecked")
        Mono<AuthLoginResponse> result = (Mono<AuthLoginResponse>) method.invoke(authService, userInfo);
        return result.block();
    }
}
