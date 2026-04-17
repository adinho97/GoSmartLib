package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Service
public class AuthService {

        private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

        private final WebClient webClient;
        private final SmartschoolProperties smartschoolProperties;
        private final AppUserRepository appUserRepository;
        private final ObjectMapper objectMapper;

        public AuthService(WebClient.Builder webClientBuilder,
                        SmartschoolProperties smartschoolProperties,
                        AppUserRepository appUserRepository,
                        ObjectMapper objectMapper) {
                this.webClient = webClientBuilder.build();
                this.smartschoolProperties = smartschoolProperties;
                this.appUserRepository = appUserRepository;
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
                return getAccessToken(code)
                                .flatMap(tokenResponse -> getUserInfo(tokenResponse, null))
                                .map(this::saveUserAndBuildResponse);
        }

        public Mono<Void> logout(String accessToken) {
                return revokeSmartschoolToken(accessToken)
                                .doOnSuccess(v -> logger.info("User logged out and token revoked successfully"))
                                .doOnError(error -> logger.error("Error during logout, but proceeding anyway", error))
                                .onErrorResume(error -> Mono.empty()); // Continue even if revocation fails
        }

        public Mono<Boolean> validateToken(String accessToken) {
                return this.webClient.get()
                                .uri(smartschoolProperties.getApiBaseUrl() + "/Api/V1/userinfo")
                                .headers(headers -> headers.setBearerAuth(accessToken))
                                .retrieve()
                                .toBodilessEntity()
                                .then(Mono.just(true))
                                .doOnSuccess(v -> logger.info("Token validation successful"))
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

        private AuthLoginResponse saveUserAndBuildResponse(SmartschoolUserInfo userInfo) {
                String sub = userInfo.getSub();
                String smartschoolRole = userInfo.getRole();
                String displayName = userInfo.getName(); // Renamed to clarify it's for display only

                // Check if user already exists
                var existingUserOpt = appUserRepository.findBySub(sub);
                String finalRole;

                if (existingUserOpt.isPresent()) {
                        // User exists → keep existing role (don't overwrite with Smartschool role)
                        // This preserves bibbeheerder role after promotion
                        finalRole = existingUserOpt.get().getRole();
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

                // Always update tokens and platform (regardless of existing or new)
                user.setRole(finalRole);
                user.setSmartschoolRefreshToken(userInfo.getRefreshToken());
                user.setAccessToken(userInfo.getAccessToken());
                user.setPlatform(userInfo.getPlatform());
                appUserRepository.save(user);

                AuthLoginResponse response = new AuthLoginResponse(
                                sub,
                                finalRole,
                                displayName,
                                userInfo.getGivenName(),
                                userInfo.getFamilyName());
                response.setAccessToken(userInfo.getAccessToken());
                return response;
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

                String tokenUrl = smartschoolProperties.getApiBaseUrl() + "/OAuth/index/token";
                logger.info("Requesting access token from: {}", tokenUrl);

                return this.webClient.post()
                                .uri(tokenUrl)
                                .header("User-Agent", "GoSmartLib-Backend")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
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

                String baseUrl = (platformUrl != null && !platformUrl.isBlank())
                                ? platformUrl
                                : smartschoolProperties.getApiBaseUrl();
                String tokenUrl = baseUrl + "/OAuth/index/token";
                logger.info("Requesting new access token using refresh token from: {}", tokenUrl);

                return this.webClient.post()
                                .uri(tokenUrl)
                                .header("User-Agent", "GoSmartLib-Backend")
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
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
                String baseUrl = (platformUrl != null && !platformUrl.isBlank())
                                ? platformUrl
                                : smartschoolProperties.getApiBaseUrl();

                return this.webClient.get()
                                .uri(baseUrl + "/Api/V1/userinfo")
                                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
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
                                        userInfo.setAccessToken(tokenResponse.getAccessToken());
                                        userInfo.setRefreshToken(tokenResponse.getRefreshToken());
                                        return userInfo;
                                })
                                .flatMap(userInfo -> this.enrichUserRoleFromGroupInfo(userInfo,
                                                tokenResponse.getAccessToken(), baseUrl))
                                .doOnSuccess(userInfo -> logger.info("Successfully retrieved user info for user: {}",
                                                userInfo.getName()))
                                .doOnError(error -> logger.error("Failed to retrieve user info", error.getMessage()));
        }
}