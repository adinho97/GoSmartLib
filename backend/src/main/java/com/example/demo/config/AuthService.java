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
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", smartschoolProperties.getRedirectUri());

        return this.webClient.post()
                .uri(smartschoolProperties.getApiBaseUrl() + "/OAuth/Token")
                .headers(headers -> headers.setBasicAuth(smartschoolProperties.getClientId(),
                        smartschoolProperties.getClientSecret(), StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(SmartschoolTokenResponse.class)
                                .doOnSuccess(token -> logger.info("Successfully retrieved access token"));
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error from Smartschool token endpoint: {} {}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("Error from Smartschool token endpoint: " + body));
                                });
                    }
                })
                .doOnError(error -> logger.error("Failed to retrieve access token", error));
    }

    private Mono<SmartschoolUserInfo> getUserInfo(SmartschoolTokenResponse tokenResponse) {
        return this.webClient.get()
                .uri(smartschoolProperties.getApiBaseUrl() + "/Api/V1/userinfo")
                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(SmartschoolUserInfo.class)
                                .doOnSuccess(userInfo -> logger.info("Successfully retrieved user info for user: {}", userInfo.getName()));
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Error from Smartschool userinfo endpoint: {} {}", response.statusCode(), body);
                                    return Mono.error(new RuntimeException("Error from Smartschool userinfo endpoint: " + body));
                                });
                    }
                })
                .doOnError(error -> logger.error("Failed to retrieve user info", error));
    }
}