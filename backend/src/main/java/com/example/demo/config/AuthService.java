package com.example.demo.config;

import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
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

    public AuthService(WebClient.Builder webClientBuilder,
                       SmartschoolProperties smartschoolProperties,
                       AppUserRepository appUserRepository) {
        this.webClient = webClientBuilder.build();
        this.smartschoolProperties = smartschoolProperties;
        this.appUserRepository = appUserRepository;
    }

    public Mono<AuthLoginResponse> processSmartschoolCallback(String code) {
        return getAccessToken(code)
                .flatMap(this::getUserInfo)
                .map(this::saveUserAndBuildResponse);
    }

    private AuthLoginResponse saveUserAndBuildResponse(SmartschoolUserInfo userInfo) {
        String sub = userInfo.getSub();
        String role = userInfo.getRole();
        String username = userInfo.getName(); // Smartschool 'name' = username (bv. petersp)

        // Zoek bestaande user of maak nieuwe aan — sla NOOIT naam op
        AppUser user = appUserRepository.findBySub(sub).orElseGet(() -> {
            AppUser newUser = new AppUser();
            newUser.setSub(sub);
            newUser.setRole(role);
            newUser.setUsername(username);
            logger.info("Nieuwe gebruiker aangemaakt met sub: {}", sub);
            return newUser;
        });

        // Update role en username bij elke login (kan veranderen)
        user.setRole(role);
        user.setUsername(username);
        appUserRepository.save(user);

        // Geef naam terug aan frontend maar sla die NOOIT op in DB
        return new AuthLoginResponse(
            sub,
            role,
            username,
            userInfo.getGivenName(),
            userInfo.getFamilyName()
        );
    }

    // --- Bestaande methodes ongewijzigd ---

    private Mono<SmartschoolTokenResponse> getAccessToken(String code) {
        String clientSecret = smartschoolProperties.getClientSecret();
        if (clientSecret == null || clientSecret.isBlank() || "${SMARTSCHOOL_CLIENT_SECRET}".equals(clientSecret)) {
            logger.error("SMARTSCHOOL_CLIENT_SECRET environment variable is not set or empty.");
            return Mono.error(new IllegalStateException("Smartschool client secret is not configured on the server."));
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
                                    logger.error("Non-2xx response from token endpoint. Status: {}, Body: {}",
                                            response.statusCode(), body);
                                    return Mono.error(new RuntimeException(
                                            "Error from Smartschool token endpoint. Status: " + response.statusCode()));
                                });
                    }
                    if (response.headers().contentType().map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML)).orElse(false)) {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new RuntimeException(
                                        "Smartschool returned HTML at token endpoint: " + body)));
                    }
                    return response.bodyToMono(SmartschoolTokenResponse.class)
                            .doOnSuccess(token -> logger.info("Successfully retrieved access token"));
                })
                .doOnError(error -> logger.error("Failed to retrieve access token", error));
    }

    private Mono<SmartschoolUserInfo> getUserInfo(SmartschoolTokenResponse tokenResponse) {
        return this.webClient.get()
                .uri(smartschoolProperties.getApiBaseUrl() + "/Api/V1/userinfo")
                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        if (response.headers().contentType().map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML)).orElse(false)) {
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new RuntimeException(
                                            "Smartschool userinfo returned HTML: " + body)));
                        }
                        return response.bodyToMono(SmartschoolUserInfo.class);
                    }
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("[no body]")
                            .flatMap(body -> Mono.error(new RuntimeException(
                                    "Error from userinfo endpoint. Status: " + response.statusCode())));
                })
                .doOnSuccess(userInfo -> logger.info("Successfully retrieved user info for: {}", userInfo.getName()))
                .doOnError(error -> logger.error("Failed to retrieve user info", error.getMessage()));
    }
}