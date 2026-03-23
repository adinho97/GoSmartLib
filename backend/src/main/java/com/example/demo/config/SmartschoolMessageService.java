package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Service
public class SmartschoolMessageService {

    private static final Logger logger = LoggerFactory.getLogger(SmartschoolMessageService.class);
    private final WebClient webClient;
    private final SmartschoolProperties smartschoolProperties;

    public SmartschoolMessageService(WebClient.Builder webClientBuilder, SmartschoolProperties smartschoolProperties) {
        this.webClient = webClientBuilder.build();
        this.smartschoolProperties = smartschoolProperties;
    }
    public Mono<String> sendMessage(String accessToken, SmartschoolMessageRequest request) {
        String messagesUrl = request.getPlatformUrl() + "/Api/V1/messages";

        // Construct the JSON payload required by Smartschool
        Map<String, String> payload = Map.of(
                "userIdentifier", request.getRecipientId(),
                "title", request.getSubject(),
                "body", request.getBody());

        logger.info("Sending Smartschool message to recipient: {}", request.getRecipientId());

        return webClient.post()
                .uri(messagesUrl)
                .header("Authorization", "Bearer " + accessToken)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .exchangeToMono(response -> {
                    if (response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("Message sent successfully");
                    } else {
                        return response.bodyToMono(String.class)
                                .flatMap(body -> {
                                    logger.error("Failed to send message. Status: {}, Body: {}", response.statusCode(),
                                            body);
                                    return Mono.error(
                                            new RuntimeException("Smartschool API error: " + response.statusCode()));
                                });
                    }
                });
    }
}