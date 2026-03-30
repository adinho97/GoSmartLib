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
                return Mono.justOrEmpty(appUserRepository.findBySub(sub))
                                .switchIfEmpty(Mono.error(new RuntimeException("User not found for sub: " + sub)))
                                .flatMap(user -> {
                                        if (user.getSmartschoolRefreshToken() == null) {
                                                return Mono.error(new RuntimeException(
                                                                "No refresh token available for user: " + sub));
                                        }
                                        return refreshAccessToken(user.getSmartschoolRefreshToken());
                                })
                                .flatMap(this::getUserInfo);
        }

        public Mono<AuthLoginResponse> processSmartschoolCallback(String code) {
                return getAccessToken(code)
                                .flatMap(this::getUserInfo)
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
                String role = userInfo.getRole();
                String displayName = userInfo.getName(); // Renamed to clarify it's for display only

                AppUser user = appUserRepository.findBySub(sub).orElseGet(() -> {
                        AppUser newUser = new AppUser();
                        newUser.setSub(sub);
                        newUser.setRole(role);
                        logger.info("Nieuwe gebruiker aangemaakt met sub: {}", sub);
                        return newUser;
                });

                user.setRole(role);
                user.setSmartschoolRefreshToken(userInfo.getRefreshToken());
                appUserRepository.save(user);

                AuthLoginResponse response = new AuthLoginResponse(
                                sub,
                                role,
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

                String tokenUrl = smartschoolProperties.getApiBaseUrl() + "/OAuth/index/token";
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

        public Mono<SmartschoolUserInfo> getUserInfo(SmartschoolTokenResponse tokenResponse) {
                return this.webClient.get()
                                .uri(smartschoolProperties.getApiBaseUrl() + "/Api/V1/userinfo")
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
                                .doOnSuccess(userInfo -> logger.info("Successfully retrieved user info for user: {}",
                                                userInfo.getName()))
                                .doOnError(error -> logger.error("Failed to retrieve user info", error.getMessage()));
        }
}