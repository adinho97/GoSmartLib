package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final WebClient webClient;
    private final SmartschoolProperties smartschoolProperties;

    public AuthService(WebClient.Builder webClientBuilder, SmartschoolProperties smartschoolProperties) {
        this.webClient = webClientBuilder.build();
        this.smartschoolProperties = smartschoolProperties;
    }

    public Mono<SmartschoolUserInfo> processSmartschoolCallback(String code) {
        return getAccessToken(code)
                .flatMap(this::getUserInfo);
    }

    private Mono<SmartschoolTokenResponse> getAccessToken(String code) {
        // Fail-fast if the client secret is not configured
        String clientSecret = smartschoolProperties.getClientSecret();
        if (clientSecret == null || clientSecret.isBlank() || "${SMARTSCHOOL_CLIENT_SECRET}".equals(clientSecret)) {
            logger.error("SMARTSCHOOL_CLIENT_SECRET environment variable is not set or empty.");
            return Mono.error(new IllegalStateException("Smartschool client secret is not configured on the server."));
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", smartschoolProperties.getRedirectUri());

        String tokenUrl = smartschoolProperties.getApiBaseUrl() + "/OAuth/token";
        logger.info("Requesting access token from: {}", tokenUrl);

        return this.webClient.post()
                .uri(tokenUrl)
                .header("User-Agent", "GoSmartLib-Backend")
                .headers(headers -> headers.setBasicAuth(smartschoolProperties.getClientId(), clientSecret))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchangeToMono(response -> {
                    // Handle non-successful responses first
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("[no body]")
                                .flatMap(body -> {
                                    logger.error(
                                            "Non-2xx response from Smartschool token endpoint. Status: {}, Headers: {}, Body: {}",
                                            response.statusCode(), response.headers().asHttpHeaders(), body);
                                    return Mono.error(new RuntimeException(
                                            "Error from Smartschool token endpoint. Status: " + response.statusCode()));
                                });
                    }

                    // Handle successful responses that are unexpectedly HTML
                    if (response.headers().contentType().map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML))
                            .orElse(false)) {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error(
                                            "Smartschool returned HTML on a 2xx response. Status: {}, Headers: {}, Body: {}",
                                            response.statusCode(), response.headers().asHttpHeaders(), body);
                                    return Mono.error(new RuntimeException(
                                            "Smartschool returned HTML at token endpoint: " + body));
                                });
                    }
                    // Happy path: successful JSON response
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
                        // Handle successful responses that are unexpectedly HTML
                        if (response.headers().contentType().map(mt -> mt.isCompatibleWith(MediaType.TEXT_HTML))
                                .orElse(false)) {
                            return response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        logger.error(
                                                "Smartschool userinfo returned HTML on a 2xx response. Status: {}, Headers: {}, Body: {}",
                                                response.statusCode(), response.headers().asHttpHeaders(), body);
                                        return Mono.error(
                                                new RuntimeException("Smartschool userinfo returned HTML: " + body));
                                    });
                        }
                        // Happy path
                        return response.bodyToMono(SmartschoolUserInfo.class);
                    }
                    // Handle non-successful responses
                    return response.bodyToMono(String.class)
                            .defaultIfEmpty("[no body]")
                            .flatMap(body -> {
                                logger.error(
                                        "Non-2xx response from Smartschool userinfo endpoint. Status: {}, Headers: {}, Body: {}",
                                        response.statusCode(), response.headers().asHttpHeaders(), body);
                                return Mono.error(new RuntimeException(
                                        "Error from Smartschool userinfo endpoint. Status: " + response.statusCode()));
                            });
                })
                .doOnSuccess(
                        userInfo -> logger.info("Successfully retrieved user info for user: {}", userInfo.getName()))
                .doOnError(error -> logger.error("Failed to retrieve user info", error.getMessage()));
    }
}