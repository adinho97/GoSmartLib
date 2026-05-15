package com.example.demo.proxy;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@RestController
@RequestMapping("/api/proxy")
public class ProxyController {

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping("/openlibrary/books")
    public ResponseEntity<String> proxyBooks(
            @RequestParam String bibkeys,
            @RequestParam String format,
            @RequestParam String jscmd) {

        String url = UriComponentsBuilder
                .fromHttpUrl("https://openlibrary.org/api/books")
                .queryParam("bibkeys", bibkeys)
                .queryParam("format", format)
                .queryParam("jscmd", jscmd)
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    @GetMapping("/openlibrary/search.json")
    public ResponseEntity<String> proxySearch(@RequestParam Map<String, String> params) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromHttpUrl("https://openlibrary.org/search.json");
        params.forEach(builder::queryParam);

        ResponseEntity<String> response = restTemplate.getForEntity(
                builder.toUriString(), String.class);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }

    @GetMapping("/openlibrary/books/{editionKey}.json")
    public ResponseEntity<String> proxyEdition(
            @org.springframework.web.bind.annotation.PathVariable String editionKey) {

        String url = UriComponentsBuilder
                .fromHttpUrl("https://openlibrary.org/books/{editionKey}.json")
                .buildAndExpand(editionKey)
                .toUriString();

        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response.getBody());
    }
}