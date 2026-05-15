package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
public class SmartschoolMessageService {

    private static final Logger logger = LoggerFactory.getLogger(SmartschoolMessageService.class);
    private final WebClient webClient;

    public SmartschoolMessageService(WebClient.Builder webClientBuilder, SmartschoolProperties smartschoolProperties) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<String> sendMessage(String accessToken, SmartschoolMessageRequest request) {
        String urlTemplate = request.getPlatformUrl() + "/Api/V1/sendmsg" +
                "?access_token={token}" +
                "&messageTitle={title}" +
                "&messageBody={body}";

        return webClient.post()
                .uri(urlTemplate, accessToken, request.getSubject(), request.getBody())
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