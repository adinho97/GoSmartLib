package com.example.demo.services;

import com.example.demo.dto.SmartschoolMessageRequest;
import com.example.demo.config.SmartschoolProperties;
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
        StringBuilder urlBuilder = new StringBuilder(request.getPlatformUrl())
                .append("/Api/V1/sendmsg")
                .append("?access_token={token}")
                .append("&messageTitle={title}")
                .append("&messageBody={body}")
                .append("&senderName={senderName}");

        Object[] vars = new Object[]{accessToken, request.getSubject(), request.getBody(), "GoSmartLib"};

        if (request.getRecipientSub() != null && !request.getRecipientSub().isBlank()) {
            urlBuilder.append("&userSub={recipientSub}");
            vars = new Object[]{accessToken, request.getSubject(), request.getBody(), "GoSmartLib", request.getRecipientSub()};
        }

        return webClient.post()
                .uri(urlBuilder.toString(), vars)
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