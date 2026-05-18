package com.example.demo.oneroster;

import com.example.demo.oneroster.OneRosterProperties.SchoolConfig;
import com.example.demo.oneroster.dto.OneRosterEnrollment;
import com.example.demo.oneroster.dto.OneRosterOrg;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Typed wrapper around the OneRoster 1.1 REST API.
 *
 * Handles auth (via {@link OneRosterTokenClient}) and offset/limit pagination
 * transparently. Callers see flat lists.
 */
@Component
public class OneRosterClient {

    private static final Logger logger = LoggerFactory.getLogger(OneRosterClient.class);

    private static final int PAGE_SIZE = 200;
    private static final int MAX_PAGES_GUARD = 10_000;

    private final WebClient webClient;
    private final OneRosterTokenClient tokenClient;
    private final OneRosterProperties properties;
    private final ObjectMapper objectMapper;

    public OneRosterClient(WebClient.Builder webClientBuilder,
            OneRosterTokenClient tokenClient,
            OneRosterProperties properties,
            ObjectMapper objectMapper) {
        this.webClient = webClientBuilder.build();
        this.tokenClient = tokenClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public Mono<OneRosterOrg> getOrg(String subdomain) {
        SchoolConfig cfg = properties.get(subdomain);
        if (cfg == null) {
            return Mono.error(new IllegalStateException(
                    "No OneRoster config for subdomain: " + subdomain));
        }
        String url = baseApiUrl(cfg) + "/orgs/" + cfg.getSchoolId();
        return tokenClient.getToken(subdomain)
                .flatMap(token -> webClient.get()
                        .uri(url)
                        .headers(h -> h.setBearerAuth(token))
                        .retrieve()
                        .bodyToMono(String.class))
                .map(json -> parseSingle(json, "org", OneRosterOrg.class));
    }

    public Mono<List<OneRosterEnrollment>> getEnrollmentsBySchool(String subdomain) {
        SchoolConfig cfg = properties.get(subdomain);
        if (cfg == null) {
            return Mono.error(new IllegalStateException(
                    "No OneRoster config for subdomain: " + subdomain));
        }
        String url = baseApiUrl(cfg) + "/schools/" + cfg.getSchoolId() + "/enrollments";
        return tokenClient.getToken(subdomain)
                .flatMap(token -> paginate(url, token, "enrollments",
                        new TypeReference<List<OneRosterEnrollment>>() {
                        }));
    }

    private <T> Mono<List<T>> paginate(String baseUrl, String token, String envelopeKey,
            TypeReference<List<T>> typeRef) {
        return paginatePage(baseUrl, token, envelopeKey, typeRef, 0, new ArrayList<>(), 0);
    }

    private <T> Mono<List<T>> paginatePage(String baseUrl, String token, String envelopeKey,
            TypeReference<List<T>> typeRef, int offset, List<T> accumulated, int pageCount) {
        if (pageCount >= MAX_PAGES_GUARD) {
            return Mono.error(new IllegalStateException(
                    "OneRoster pagination exceeded guard of " + MAX_PAGES_GUARD + " pages for " + baseUrl));
        }
        String separator = baseUrl.contains("?") ? "&" : "?";
        String url = baseUrl + separator + "limit=" + PAGE_SIZE + "&offset=" + offset;
        return webClient.get()
                .uri(url)
                .headers(h -> h.setBearerAuth(token))
                .exchangeToMono(response -> {
                    if (!response.statusCode().is2xxSuccessful()) {
                        return response.bodyToMono(String.class)
                                .defaultIfEmpty("[no body]")
                                .flatMap(body -> Mono.error(new IllegalStateException(
                                        "OneRoster request to " + url + " failed: HTTP "
                                                + response.statusCode() + " body=" + body)));
                    }
                    Long totalCount = parseTotalCount(response.headers().asHttpHeaders());
                    return response.bodyToMono(String.class)
                            .map(json -> {
                                List<T> page = parseList(json, envelopeKey, typeRef);
                                accumulated.addAll(page);
                                return new PageResult<>(page, totalCount);
                            });
                })
                .flatMap(result -> {
                    int collected = accumulated.size();
                    boolean morePagesByTotal = result.totalCount != null && collected < result.totalCount;
                    boolean morePagesByPageFull = result.totalCount == null && result.page.size() == PAGE_SIZE;
                    if (morePagesByTotal || morePagesByPageFull) {
                        return paginatePage(baseUrl, token, envelopeKey, typeRef,
                                offset + PAGE_SIZE, accumulated, pageCount + 1);
                    }
                    logger.debug("OneRoster paginated fetch of {} complete: {} items in {} pages",
                            envelopeKey, accumulated.size(), pageCount + 1);
                    return Mono.just(accumulated);
                });
    }

    private static Long parseTotalCount(HttpHeaders headers) {
        String value = headers.getFirst("X-Total-Count");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private <T> T parseSingle(String json, String envelopeKey, Class<T> type) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(envelopeKey);
            if (node == null || node.isNull()) {
                throw new IllegalStateException(
                        "OneRoster response missing '" + envelopeKey + "' key: " + truncate(json));
            }
            return objectMapper.treeToValue(node, type);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse OneRoster '" + envelopeKey + "' response: " + e.getMessage(), e);
        }
    }

    private <T> List<T> parseList(String json, String envelopeKey, TypeReference<List<T>> typeRef) {
        try {
            JsonNode root = objectMapper.readTree(json);
            JsonNode node = root.get(envelopeKey);
            if (node == null || node.isNull()) {
                return new ArrayList<>();
            }
            return objectMapper.convertValue(node, typeRef);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to parse OneRoster '" + envelopeKey + "' list: " + e.getMessage(), e);
        }
    }

    private static String baseApiUrl(SchoolConfig cfg) {
        String base = cfg.getBaseUrl();
        if (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        return base + "/v1p1";
    }

    private static String truncate(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 200 ? s.substring(0, 200) + "..." : s;
    }

    private record PageResult<T>(List<T> page, Long totalCount) {
    }
}
