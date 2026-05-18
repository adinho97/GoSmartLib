package com.example.demo.oneroster;

import com.example.demo.oneroster.OneRosterProperties.SchoolConfig;
import com.example.demo.oneroster.dto.OneRosterTokenResponse;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Per-subdomain OAuth2 client_credentials token cache for OneRoster.
 *
 * Two cooperating maps:
 *   tokens   — TTL cache. Populated only on successful fetch. Read only when
 *              expiresAt - now > 60s. Failures never poison it.
 *   inflight — concurrent-fetch dedup. Lives only for the duration of an
 *              in-flight HTTP call. doFinally(remove) always clears it, so a
 *              failed fetch doesn't get replayed to the next caller — they
 *              find no entry in inflight (because it was removed) and no
 *              entry in tokens (because we only populate on success), so they
 *              start a clean fetch.
 *
 * Why we don't reuse AuthService.refreshCache: that map is dedup-only (entries
 * wiped on doFinally), so every call would re-authenticate. For OneRoster,
 * where every list query touches the token, that's prohibitive.
 */
@Component
public class OneRosterTokenClient {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterTokenClient.class);

    private static final long EXPIRY_SAFETY_BUFFER_SECONDS = 60;

    private record CachedToken(String accessToken, Instant expiresAt) {
    }

    private final ConcurrentMap<String, CachedToken> tokens = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Mono<CachedToken>> inflight = new ConcurrentHashMap<>();

    private final WebClient webClient;
    private final OneRosterProperties properties;

    public OneRosterTokenClient(WebClient.Builder webClientBuilder, OneRosterProperties properties) {
        this.webClient = webClientBuilder.build();
        this.properties = properties;
    }

    public Mono<String> getToken(String subdomain) {
        return Mono.defer(() -> {
            CachedToken cached = tokens.get(subdomain);
            if (cached != null && Instant.now().isBefore(cached.expiresAt().minusSeconds(EXPIRY_SAFETY_BUFFER_SECONDS))) {
                return Mono.just(cached);
            }
            return inflight.computeIfAbsent(subdomain, k ->
                    fetchNewToken(subdomain)
                            .doOnNext(t -> tokens.put(subdomain, t))
                            .doFinally(s -> inflight.remove(subdomain))
                            .cache());
        }).map(CachedToken::accessToken);
    }

    /**
     * Visible for tests: clears all cached state for a subdomain so a test can
     * simulate a cold start without restarting Spring.
     */
    void invalidate(String subdomain) {
        tokens.remove(subdomain);
        inflight.remove(subdomain);
    }

    private Mono<CachedToken> fetchNewToken(String subdomain) {
        SchoolConfig cfg = properties.get(subdomain);
        if (cfg == null || !cfg.isUsable()) {
            return Mono.error(new IllegalStateException(
                    "OneRoster config missing or incomplete for subdomain: " + subdomain));
        }

        String tokenUrl = stripTrailingSlash(cfg.getBaseUrl()) + "/token";
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "client_credentials");
        formData.add("client_id", cfg.getClientId());
        formData.add("client_secret", cfg.getClientSecret());

        logger.debug("Requesting OneRoster token for subdomain {} from {}", subdomain, tokenUrl);

        MediaType formContentType = Objects.requireNonNull(MediaType.APPLICATION_FORM_URLENCODED);

        return webClient.post()
                .uri(tokenUrl)
                .contentType(formContentType)
                .header("User-Agent", "GoSmartLib-Backend")
                .bodyValue(formData)
                .exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("[no body]")
                                .flatMap(body -> {
                                    logger.error("OneRoster token endpoint returned {} for {}: {}",
                                            response.statusCode(), subdomain, body);
                                    return Mono.error(new IllegalStateException(
                                            "OneRoster token request failed for " + subdomain
                                                    + ": HTTP " + response.statusCode()));
                                });
                    }
                    return response.bodyToMono(OneRosterTokenResponse.class);
                })
                .map(resp -> {
                    long expiresInSeconds = resp.getExpiresIn() != null ? resp.getExpiresIn() : 3600L;
                    Instant expiresAt = Instant.now().plusSeconds(expiresInSeconds);
                    logger.info("Fetched OneRoster token for {} (expires in {}s)", subdomain, expiresInSeconds);
                    return new CachedToken(resp.getAccessToken(), expiresAt);
                });
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
