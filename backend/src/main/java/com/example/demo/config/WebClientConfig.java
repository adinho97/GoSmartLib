package com.example.demo.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import io.netty.resolver.DefaultAddressResolverGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import javax.net.ssl.SSLException;
import java.util.concurrent.TimeUnit;

@Configuration
public class WebClientConfig {
    private static final Logger logger = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    public WebClient.Builder webClientBuilder() throws SSLException {
        logger.info("Initializing WebClient with custom timeout and SSL configuration");
        
        ConnectionProvider connectionProvider = ConnectionProvider.builder("custom")
                .maxConnections(500)
                .maxIdleTime(java.time.Duration.ofSeconds(20))
                .build();

        var sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();

        HttpClient httpClient = HttpClient.create(connectionProvider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 15000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .responseTimeout(java.time.Duration.ofSeconds(60))
                .secure(spec -> spec.sslContext(sslContext))
                .resolver(DefaultAddressResolverGroup.INSTANCE)
                .doOnConnected(conn -> {
                    conn.addHandlerLast(new ReadTimeoutHandler(60, TimeUnit.SECONDS));
                    conn.addHandlerLast(new WriteTimeoutHandler(60, TimeUnit.SECONDS));
                    logger.debug("WebClient connection established");
                });

        logger.info("WebClient configured with: 15s connection timeout, 60s response timeout, SSL verification disabled");
        
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}