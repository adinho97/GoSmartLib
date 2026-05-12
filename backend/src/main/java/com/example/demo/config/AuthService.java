package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.entities.Klas;
import com.example.demo.entities.School;
import com.example.demo.entities.SchoolStatus;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.KlasRepository;
import com.example.demo.repositories.SchoolRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class AuthService {

        private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

        private final WebClient webClient;
        private final SmartschoolProperties smartschoolProperties;
        private final AppUserRepository appUserRepository;
        private final SchoolRepository schoolRepository;
        private final KlasRepository klasRepository;
        private final ObjectMapper objectMapper;

        public AuthService(WebClient.Builder webClientBuilder,
                        SmartschoolProperties smartschoolProperties,
                        AppUserRepository appUserRepository,
                        SchoolRepository schoolRepository,
                        KlasRepository klasRepository,
                        ObjectMapper objectMapper) {
                this.webClient = webClientBuilder.build();
                this.smartschoolProperties = smartschoolProperties;
                this.appUserRepository = appUserRepository;
                this.schoolRepository = schoolRepository;
                this.klasRepository = klasRepository;
                this.objectMapper = objectMapper;
        }

        public Mono<SmartschoolUserInfo> getUserInfoBySub(String sub) {
                logger.info("Looking up user by sub: {}", sub);
                return Mono.justOrEmpty(appUserRepository.findBySub(sub))
                                .switchIfEmpty(Mono.error(new RuntimeException("User not found for sub: " + sub)))
                                .flatMap(user -> {
                                        logger.info("User found. ID: {}, has refresh token: {}",
                                                        user.getId(), user.getSmartschoolRefreshToken() != null);
                                        if (user.getSmartschoolRefreshToken() == null) {
                                                logger.error("No refresh token available for user: {}", sub);
                                                return Mono.error(new RuntimeException(
                                                                "No refresh token available for user: " + sub));
                                        }
                                        logger.debug("Refreshing access token for user: {}", sub);
                                        return refreshAccessToken(user.getSmartschoolRefreshToken(), user.getPlatform())
                                                        .flatMap(tokenResponse -> {
                                                                user.setAccessToken(tokenResponse.getAccessToken());
                                                                if (tokenResponse.getRefreshToken() != null) {
                                                                        user.setSmartschoolRefreshToken(tokenResponse
                                                                                        .getRefreshToken());
                                                                }
                                                                appUserRepository.save(user);
                                                                logger.info("Token refreshed successfully for user: {}",
                                                                                sub);
                                                                return getUserInfo(tokenResponse, user.getPlatform());
                                                        });
                                });
        }

        public Mono<AuthLoginResponse> processSmartschoolCallback(String code) {
                logger.info("Processing Smartschool callback with code length: {}", code != null ? code.length() : 0);
                if (code == null || code.isBlank()) {
                        logger.error("Received empty or null code in processSmartschoolCallback");
                        return Mono.error(new IllegalArgumentException("Authorization code is required"));
                }
                return getAccessToken(code)
                                .flatMap(tokenResponse -> getUserInfo(tokenResponse, null))
                                .flatMap(this::saveUserAndBuildResponse);
        }

        public Mono<Void> logout(String accessToken) {
                return revokeSmartschoolToken(accessToken)
                                .doOnSuccess(v -> logger.info("User logged out and token revoked successfully"))
                                .doOnError(error -> logger.error("Error during logout, but proceeding anyway", error))
                                .onErrorResume(error -> Mono.empty())
                                .then(Mono.fromRunnable(() ->
                                        appUserRepository.findByAccessToken(accessToken).ifPresent(user -> {
                                                user.setAccessToken(null);
                                                appUserRepository.save(user);
                                        })
                                ));
        }

        public Mono<Boolean> validateToken(String accessToken) {
                String accessTokenValue = Objects.requireNonNull(accessToken, "accessToken");
                
                // First, check if token exists in our database (AppUser.access_token)
                // This allows validation even if Smartschool API is unreachable
                java.util.Optional<AppUser> userOpt = appUserRepository.findByAccessToken(accessTokenValue);
                if (userOpt.isPresent()) {
                        AppUser user = userOpt.get();
                        if (user.isActive()) {
                                logger.debug("Token validation successful (found in database)");
                                return Mono.just(true);
                        }
                }
                
                // If not in database or user is inactive, validate against Smartschool API
                return this.webClient.get()
                                .uri(smartschoolProperties.getApiBaseUrl() + "/Api/V1/userinfo")
                                .headers(headers -> headers.setBearerAuth(accessTokenValue))
                                .retrieve()
                                .toBodilessEntity()
                                .then(Mono.just(true))
                                .doOnSuccess(v -> logger.info("Token validation successful (from Smartschool API)"))
                                .onErrorResume(error -> {
                                        logger.debug("Token validation failed: {}", error.getMessage());
                                        return Mono.just(false);
                                });
        }

        private Mono<Void> revokeSmartschoolToken(String accessToken) {
                String revokeUrl = smartschoolProperties.getApiBaseUrl() + "/Api/V1/revoke";
                logger.info("Revoking access token at: {}", revokeUrl);

                return this.webClient.post()
                                .uri(revokeUrl + "?access_token=" + accessToken)
                                .retrieve()
                                .toBodilessEntity()
                                .doOnSuccess(response -> logger.info("Token revoked successfully"))
                                .doOnError(error -> logger.warn("Failed to revoke token: {}", error.getMessage()))
                                .then(); // Return empty Mono<Void>
        }

        private Mono<AuthLoginResponse> saveUserAndBuildResponse(SmartschoolUserInfo userInfo) {
                String sub = userInfo.getSub();
                String smartschoolRole = userInfo.getRole();
                String displayName = userInfo.getName(); // Renamed to clarify it's for display only

                // Check if user already exists
                Optional<AppUser> existingUserOpt = appUserRepository.findBySub(sub);
                String finalRole;

                if (existingUserOpt.isPresent()) {
                        AppUser existing = existingUserOpt.get();
                        if (!existing.isActive()) {
                                throw new ApiException(
                                                "Uw account is gedeactiveerd. Neem contact op met uw schoolbeheerder.",
                                                HttpStatus.FORBIDDEN, "USER_INACTIVE");
                        }
                        // Keep existing role — preserves bibbeheerder after promotion
                        finalRole = existing.getRole();
                        logger.info("Bestaande gebruiker ingelogd met sub: {}, keeping role: {}", sub, finalRole);
                } else {
                        // New user → set role from Smartschool or fallback to leerling
                        finalRole = smartschoolRole != null ? smartschoolRole : "leerling";
                        logger.info("Nieuwe gebruiker aangemaakt met sub: {}, role: {}", sub, finalRole);
                }

                AppUser user = existingUserOpt.orElseGet(() -> {
                        AppUser newUser = new AppUser();
                        newUser.setSub(sub);
                        return newUser;
                });

                String normalizedPlatform = normalizePlatformUrl(userInfo.getPlatform());
                String subdomain = extractSubdomain(normalizedPlatform);
                School school = resolveSchoolForLogin(existingUserOpt, normalizedPlatform, subdomain);

                if (school.getStatus() == SchoolStatus.INACTIVE) {
                        throw new ApiException("School is gedeactiveerd", HttpStatus.FORBIDDEN, "SCHOOL_INACTIVE");
                }
                if (school.getStatus() == SchoolStatus.PENDING) {
                        school.setStatus(SchoolStatus.ACTIVE);
                        school = schoolRepository.save(school);
                }

                final School resolvedSchool = school;
                final String roleToPersist = finalRole;
                final AppUser targetUser = user;
                final String platformForUser = normalizedPlatform;

                return getGroupInfo(userInfo.getAccessToken(), normalizedPlatform)
                                .onErrorResume(error -> {
                                        logger.warn("Failed to fetch groups during login, continuing without class sync",
                                                        error);
                                        return Mono.just(new SmartschoolGroupInfo());
                                })
                                .map(groupInfo -> {
                                        Klas primaryKlas = upsertKlasData(resolvedSchool, groupInfo.getGroups());

                                        targetUser.setRole(roleToPersist);
                                        targetUser.setSmartschoolRefreshToken(userInfo.getRefreshToken());
                                        targetUser.setAccessToken(userInfo.getAccessToken());
                                        targetUser.setPlatform(platformForUser);
                                        targetUser.setSchool(resolvedSchool);
                                        if (primaryKlas != null) {
                                                targetUser.setKlas(primaryKlas);
                                        }
                                        appUserRepository.save(targetUser);

                                        AuthLoginResponse response = new AuthLoginResponse(
                                                        sub,
                                                        roleToPersist,
                                                        displayName,
                                                        userInfo.getGivenName(),
                                                        userInfo.getFamilyName());
                                        response.setAccessToken(userInfo.getAccessToken());
                                        response.setSchoolId(resolvedSchool.getId());
                                        response.setSchoolNaam(resolvedSchool.getNaam());
                                        return response;
                                });
        }

        private School resolveSchoolForLogin(Optional<AppUser> existingUserOpt, String normalizedPlatform, String subdomain) {
                Optional<School> schoolOpt = schoolRepository.findBySubdomeinIgnoreCase(subdomain);
                if (schoolOpt.isPresent()) {
                        return schoolOpt.get();
                }

                // Compatibility bridge: keep legacy users (with existing app data like loans)
                // able to log in even before manual school registration migration is complete.
                if (existingUserOpt.isPresent()) {
                        logger.warn("Legacy user login without registered school for subdomain {}, auto-creating school",
                                        subdomain);
                        School legacySchool = new School();
                        legacySchool.setSubdomein(subdomain);
                        legacySchool.setSmartschoolUrl(normalizedPlatform);
                        legacySchool.setNaam(subdomain);
                        legacySchool.setStatus(SchoolStatus.ACTIVE);
                        return schoolRepository.save(legacySchool);
                }

                throw new ApiException("Jouw school is nog niet geregistreerd", HttpStatus.FORBIDDEN,
                                "SCHOOL_NOT_REGISTERED");
        }

        private String normalizePlatformUrl(String platform) {
                if (platform == null || platform.isBlank()) {
                        throw new ApiException("Ongeldige schoolplatform informatie ontvangen", HttpStatus.BAD_REQUEST,
                                        "INVALID_PLATFORM");
                }
                String trimmed = platform.trim();
                if (!trimmed.startsWith("http://") && !trimmed.startsWith("https://")) {
                        trimmed = "https://" + trimmed;
                }
                return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
        }

        private String extractSubdomain(String platformUrl) {
                try {
                        URI uri = URI.create(platformUrl);
                        String host = uri.getHost();
                        if (host == null || !host.toLowerCase(Locale.ROOT).endsWith(".smartschool.be")) {
                                throw new ApiException("Ongeldig Smartschool platform domein", HttpStatus.BAD_REQUEST,
                                                "INVALID_PLATFORM");
                        }
                        return host.substring(0, host.length() - ".smartschool.be".length()).toLowerCase(Locale.ROOT);
                } catch (IllegalArgumentException ex) {
                        throw new ApiException("Ongeldig Smartschool platform domein", HttpStatus.BAD_REQUEST,
                                        "INVALID_PLATFORM");
                }
        }

        private Klas upsertKlasData(School school, List<SmartschoolGroup> groups) {
                if (groups == null || groups.isEmpty()) {
                        return null;
                }

                Klas primary = null;
                for (SmartschoolGroup group : groups) {
                        if (group == null || group.getGroupID() == null || group.getGroupID().isBlank()) {
                                continue;
                        }
                        String cleanedGroupId = group.getGroupID().trim();
                        String klasNaam = group.getName() == null || group.getName().isBlank()
                                        ? cleanedGroupId
                                        : group.getName().trim();

                        Klas klas = klasRepository.findBySchool_IdAndGroupId(school.getId(), cleanedGroupId)
                                        .orElseGet(() -> {
                                                Klas created = new Klas();
                                                created.setSchool(school);
                                                created.setGroupId(cleanedGroupId);
                                                return created;
                                        });
                        klas.setNaam(klasNaam);
                        Klas saved = klasRepository.save(klas);

                        if (primary == null) {
                                primary = saved;
                        }
                }
                return primary;
        }

        private Mono<SmartschoolTokenResponse> getAccessToken(String code) {
                String clientSecret = smartschoolProperties.getClientSecret();
                if (clientSecret == null || clientSecret.isBlank()
                                || "${SMARTSCHOOL_CLIENT_SECRET}".equals(clientSecret)) {
                        logger.error("SMARTSCHOOL_CLIENT_SECRET environment variable is not set or empty.");
                        return Mono.error(new IllegalStateException(
                                        "Smartschool client secret is not configured on the server."));
                }

                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "authorization_code");
                formData.add("code", code);
                formData.add("redirect_uri", smartschoolProperties.getRedirectUri());
                formData.add("client_id", smartschoolProperties.getClientId());
                formData.add("client_secret", clientSecret);
                MediaType formContentType = Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED);

                String tokenUrl = smartschoolProperties.getApiBaseUrl() + "/OAuth/index/token";
                logger.info("Requesting access token from: {}", tokenUrl);
                logger.debug("OAuth request parameters - grant_type: authorization_code, client_id: {}, redirect_uri: {}, code length: {}",
                                smartschoolProperties.getClientId(),
                                smartschoolProperties.getRedirectUri(),
                                code != null ? code.length() : 0);

                return this.webClient.post()
                                .uri(tokenUrl)
                                .header("User-Agent", "GoSmartLib-Backend")
                                .contentType(formContentType)
                                .bodyValue(formData)
                                .exchangeToMono(response -> {
                                        if (!response.statusCode().is2xxSuccessful()) {
                                                return response.bodyToMono(String.class)
                                                                .defaultIfEmpty("[no body]")
                                                                .flatMap(body -> {
                                                                        logger.error(
                                                                                        "Non-2xx response from Smartschool token endpoint. Status: {}, Headers: {}, Body: {}",
                                                                                        response.statusCode(),
                                                                                        response.headers()
                                                                                                        .asHttpHeaders(),
                                                                                        body);
                                                                        return Mono.error(new RuntimeException(
                                                                                        "Error from Smartschool token endpoint. Status: "
                                                                                                        + response.statusCode()));
                                                                });
                                        }
                                        if (response.headers().contentType()
                                                        .map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML))
                                                        .orElse(false)) {
                                                return response.bodyToMono(String.class)
                                                                .flatMap(body -> {
                                                                        logger.error(
                                                                                        "Smartschool returned HTML on a 2xx response. Status: {}, Headers: {}, Body: {}",
                                                                                        response.statusCode(),
                                                                                        response.headers()
                                                                                                        .asHttpHeaders(),
                                                                                        body);
                                                                        return Mono.error(new RuntimeException(
                                                                                        "Smartschool returned HTML at token endpoint: "
                                                                                                        + body));
                                                                });
                                        }
                                        return response.bodyToMono(SmartschoolTokenResponse.class)
                                                        .doOnSuccess(token -> logger
                                                                        .info("Successfully retrieved access token"));
                                })
                                .doOnError(error -> logger.error("Failed to retrieve access token", error));
        }

        public Mono<SmartschoolTokenResponse> refreshAccessToken(String refreshToken) {
                return refreshAccessToken(refreshToken, null);
        }

        public Mono<SmartschoolTokenResponse> refreshAccessToken(String refreshToken, String platformUrl) {
                String clientSecret = smartschoolProperties.getClientSecret();
                if (clientSecret == null || clientSecret.isBlank()
                                || "${SMARTSCHOOL_CLIENT_SECRET}".equals(clientSecret)) {
                        logger.error("SMARTSCHOOL_CLIENT_SECRET environment variable is not set or empty.");
                        return Mono.error(new IllegalStateException(
                                        "Smartschool client secret is not configured on the server."));
                }

                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("grant_type", "refresh_token");
                formData.add("refresh_token", refreshToken);
                formData.add("client_id", smartschoolProperties.getClientId());
                formData.add("client_secret", clientSecret);
                MediaType formContentType = Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED);

                String baseUrl = (platformUrl != null && !platformUrl.isBlank())
                                ? platformUrl
                                : smartschoolProperties.getApiBaseUrl();
                String tokenUrl = baseUrl + "/OAuth/index/token";
                logger.info("Requesting new access token using refresh token from: {}", tokenUrl);

                return this.webClient.post()
                                .uri(tokenUrl)
                                .header("User-Agent", "GoSmartLib-Backend")
                                .contentType(formContentType)
                                .bodyValue(formData)
                                .exchangeToMono(response -> {
                                        if (!response.statusCode().is2xxSuccessful()) {
                                                return response.bodyToMono(String.class)
                                                                .defaultIfEmpty("[no body]")
                                                                .flatMap(body -> {
                                                                        logger.error(
                                                                                        "Non-2xx response from Smartschool token endpoint (refresh). Status: {}, Headers: {}, Body: {}",
                                                                                        response.statusCode(),
                                                                                        response.headers()
                                                                                                        .asHttpHeaders(),
                                                                                        body);
                                                                        return Mono.error(new RuntimeException(
                                                                                        "Error from Smartschool token endpoint (refresh). Status: "
                                                                                                        + response.statusCode()));
                                                                });
                                        }
                                        if (response.headers().contentType()
                                                        .map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML))
                                                        .orElse(false)) {
                                                return response.bodyToMono(String.class)
                                                                .flatMap(body -> {
                                                                        logger.error(
                                                                                        "Smartschool returned HTML on a 2xx response (refresh). Status: {}, Headers: {}, Body: {}",
                                                                                        response.statusCode(),
                                                                                        response.headers()
                                                                                                        .asHttpHeaders(),
                                                                                        body);
                                                                        return Mono.error(new RuntimeException(
                                                                                        "Smartschool returned HTML at token endpoint (refresh): "
                                                                                                        + body));
                                                                });
                                        }
                                        return response.bodyToMono(SmartschoolTokenResponse.class)
                                                        .doOnSuccess(token -> logger
                                                                        .info("Successfully retrieved new access token using refresh token."));
                                })
                                .doOnError(error -> logger.error(
                                                "Failed to retrieve new access token using refresh token.", error));
        }

        private Mono<SmartschoolUserInfo> enrichUserRoleFromGroupInfo(
                        SmartschoolUserInfo userInfo,
                        String accessToken,
                        String baseUrl) {
                if (userInfo.getRole() != null && userInfo.getRole().equals("leerkracht")) {
                        return Mono.just(userInfo);
                }

                return getGroupInfo(accessToken, baseUrl)
                                .map(this::deriveRoleFromGroupInfo)
                                .map(groupRole -> {
                                        if (groupRole != null) {
                                                userInfo.setRole(groupRole);
                                        }
                                        return userInfo;
                                })
                                .onErrorResume(error -> {
                                        logger.warn("Failed to load groupinfo for role detection, using fallback role",
                                                        error);
                                        return Mono.just(userInfo);
                                });
        }

        private Mono<SmartschoolGroupInfo> getGroupInfo(String accessToken, String baseUrl) {
                String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
                String groupInfoUrl = baseUrl + "/Api/V1/groupinfo?access_token=" + encodedToken;

                return this.webClient.get()
                                .uri(groupInfoUrl)
                                .retrieve()
                                .bodyToMono(String.class)
                                .map(json -> {
                                        try {
                                                return objectMapper.readValue(json, SmartschoolGroupInfo.class);
                                        } catch (Exception e) {
                                                logger.error("Error parsing Smartschool groupinfo JSON", e);
                                                throw new RuntimeException("Failed to parse Smartschool groupinfo JSON",
                                                                e);
                                        }
                                });
        }

        private String deriveRoleFromGroupInfo(SmartschoolGroupInfo groupInfo) {
                if (groupInfo == null) {
                        return null;
                }

                var roleHints = Stream.concat(
                                groupInfo.getGroups().stream(),
                                groupInfo.getParentGroups().stream())
                                .flatMap(item -> Stream.of(item.getName(), item.getDescription()))
                                .filter(Objects::nonNull)
                                .map(String::toLowerCase)
                                .toList();

                if (roleHints.stream().anyMatch(text -> text.contains("leerkracht") || text.contains("leerkrachten")
                                || text.contains("leraar"))) {
                        return "leerkracht";
                }
                if (roleHints.stream().anyMatch(text -> text.contains("leerling") || text.contains("student"))) {
                        return "leerling";
                }
                return null;
        }

        public Mono<SmartschoolUserInfo> getUserInfo(SmartschoolTokenResponse tokenResponse, String platformUrl) {
                String accessToken = Objects.requireNonNull(tokenResponse.getAccessToken(), "accessToken");
                String baseUrl = (platformUrl != null && !platformUrl.isBlank())
                                ? platformUrl
                                : smartschoolProperties.getApiBaseUrl();

                return this.webClient.get()
                                .uri(baseUrl + "/Api/V1/userinfo")
                                .headers(headers -> headers.setBearerAuth(accessToken))
                                .exchangeToMono(response -> {
                                        if (response.statusCode().is2xxSuccessful()) {
                                                if (response.headers().contentType()
                                                                .map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML))
                                                                .orElse(false)) {
                                                        return response.bodyToMono(String.class)
                                                                        .flatMap(body -> {
                                                                                logger.error(
                                                                                                "Smartschool userinfo returned HTML on a 2xx response. Status: {}, Headers: {}, Body: {}",
                                                                                                response.statusCode(),
                                                                                                response.headers()
                                                                                                                .asHttpHeaders(),
                                                                                                body);
                                                                                return Mono.error(new RuntimeException(
                                                                                                "Smartschool userinfo returned HTML: "
                                                                                                                + body));
                                                                        });
                                                }
                                                return response.bodyToMono(String.class)
                                                                .map(json -> {
                                                                        logger.info("Raw Smartschool UserInfo Response: {}",
                                                                                        json);
                                                                        try {
                                                                                return objectMapper.readValue(json,
                                                                                                SmartschoolUserInfo.class);
                                                                        } catch (Exception e) {
                                                                                logger.error("Error parsing UserInfo JSON",
                                                                                                e);
                                                                                throw new RuntimeException(
                                                                                                "Failed to parse UserInfo",
                                                                                                e);
                                                                        }
                                                                });
                                        }
                                        return response.bodyToMono(String.class)
                                                        .defaultIfEmpty("[no body]")
                                                        .flatMap(body -> {
                                                                logger.error(
                                                                                "Non-2xx response from Smartschool userinfo endpoint. Status: {}, Headers: {}, Body: {}",
                                                                                response.statusCode(),
                                                                                response.headers().asHttpHeaders(),
                                                                                body);
                                                                return Mono.error(new RuntimeException(
                                                                                "Error from Smartschool userinfo endpoint. Status: "
                                                                                                + response.statusCode()));
                                                        });
                                })
                                .map(userInfo -> {
                                        userInfo.setAccessToken(accessToken);
                                        userInfo.setRefreshToken(tokenResponse.getRefreshToken());
                                        return userInfo;
                                })
                                .flatMap(userInfo -> this.enrichUserRoleFromGroupInfo(userInfo,
                                                accessToken, baseUrl))
                                .doOnSuccess(userInfo -> logger.info("Successfully retrieved user info for user: {}",
                                                userInfo.getName()))
                                .doOnError(error -> logger.error("Failed to retrieve user info", error.getMessage()));
        }
}