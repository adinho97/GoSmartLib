package com.example.demo.config;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Service
public class AuthService {

    private final WebClient webClient;
    private final SmartschoolProperties smartschoolProperties;

    public AuthService(WebClient.Builder webClientBuilder, SmartschoolProperties smartschoolProperties) {
        this.webClient = webClientBuilder.baseUrl("https://oauth.smartschool.be").build();
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
                .uri("/OAuth/Token")
                .headers(headers -> headers.setBasicAuth(smartschoolProperties.getClientId(),
                        smartschoolProperties.getClientSecret(), StandardCharsets.UTF_8))
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(SmartschoolTokenResponse.class);
    }

    private Mono<SmartschoolUserInfo> getUserInfo(SmartschoolTokenResponse tokenResponse) {
        return this.webClient.get()
                .uri("/Api/V1/userinfo")
                .headers(headers -> headers.setBearerAuth(tokenResponse.getAccessToken()))
                .retrieve()
                .bodyToMono(SmartschoolUserInfo.class);
    }
}