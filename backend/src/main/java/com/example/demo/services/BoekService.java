package com.example.demo.services;

import com.example.demo.BoekRepository;
import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.mappers.BoekMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class BoekService {

    private final BoekRepository boekRepository;

    public BoekService(BoekRepository boekRepository) {
        this.boekRepository = boekRepository;
    }

    public Optional<BoekDto> findByIsbn(String isbn) {
        return boekRepository.findByIsbn(isbn)
                .map(BoekMapper::toDto);
    }

    public BoekDto fetchPreviewByIsbn(String isbn) {
        Boek fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }
        return BoekMapper.toDto(fetched);
    }

    @Transactional
    public BoekDto importByIsbn(String isbn) {
        Optional<Boek> existing = boekRepository.findByIsbn(isbn);
        if (existing.isPresent()) {
            return BoekMapper.toDto(existing.get());
        }

        Boek fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }

        Boek saved = boekRepository.save(fetched);
        return BoekMapper.toDto(saved);
    }

    private Boek fetchBookFromOpenLibrary(String isbn) {
        String url = "https://openlibrary.org/isbn/" + isbn + ".json";
        RestTemplate restTemplate = new RestTemplate();

        try {
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                return null;
            }

            Map<String, Object> body = response.getBody();

            Boek book = new Boek();
            book.setIsbn(isbn);

            Object title = body.get("title");
            book.setTitel(title instanceof String ? (String) title : "Unknown title");

            // Pages
            Object pages = body.get("number_of_pages");
            if (pages instanceof Number) {
                book.setPaginas(((Number) pages).intValue());
            }

            // Description (can be string or object in OpenLibrary)
            Object description = body.get("description");
            if (description instanceof String) {
                book.setBeschrijving((String) description);
            } else if (description instanceof Map) {
                Object value = ((Map<?, ?>) description).get("value");
                if (value instanceof String) {
                    book.setBeschrijving((String) value);
                }
            }

            // Publish date (best-effort parsing)
            Object publishDate = body.get("publish_date");
            if (publishDate instanceof String publishDateStr) {
                LocalDate parsed = tryParsePublishDate(publishDateStr);
                if (parsed != null) {
                    book.setUitgaveDatum(parsed);
                }
            }

            book.setTaal("EN");

            // Cover image
            Object covers = body.get("covers");
            if (covers instanceof List<?> coverList && !coverList.isEmpty()) {
                Object first = coverList.get(0);
                if (first instanceof Number) {
                    int coverId = ((Number) first).intValue();
                    String coverUrl = "https://covers.openlibrary.org/b/id/" + coverId + "-M.jpg";
                    book.setCover(coverUrl);
                }
            }

            // Publisher
            Object publishers = body.get("publishers");
            if (publishers instanceof List<?> publisherList && !publisherList.isEmpty()) {
                Object first = publisherList.get(0);
                if (first instanceof String) {
                    book.setUitgeverij((String) first);
                }
            }

            // Author: fetch from OpenLibrary authors API
            book.setAuteur(fetchAuthorName(body, restTemplate));

            return book;
        } catch (HttpClientErrorException.NotFound e) {
            // ISBN not found in OpenLibrary
            return null;
        } catch (Exception e) {
            // Network or parsing error etc.
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String fetchAuthorName(Map<String, Object> editionBody, RestTemplate restTemplate) {
        try {
            // Edition-level authors
            Object authors = editionBody.get("authors");
            if (authors instanceof List<?> authorList && !authorList.isEmpty()) {
                Object firstAuthor = authorList.get(0);
                if (firstAuthor instanceof Map<?, ?> authorMap) {
                    Object key = authorMap.get("key");
                    if (key instanceof String authorKey) {
                        String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                        ResponseEntity<Map> authorResp = restTemplate.getForEntity(authorUrl, Map.class);
                        if (authorResp.getBody() != null) {
                            Object name = authorResp.getBody().get("name");
                            if (name instanceof String) return (String) name;
                        }
                    }
                }
            }

            // Fallback: fetch via works endpoint
            Object works = editionBody.get("works");
            if (works instanceof List<?> workList && !workList.isEmpty()) {
                Object firstWork = workList.get(0);
                if (firstWork instanceof Map<?, ?> workMap) {
                    Object key = workMap.get("key");
                    if (key instanceof String workKey) {
                        String workUrl = "https://openlibrary.org" + workKey + ".json";
                        ResponseEntity<Map> workResp = restTemplate.getForEntity(workUrl, Map.class);
                        if (workResp.getBody() != null) {
                            Object workAuthors = workResp.getBody().get("authors");
                            if (workAuthors instanceof List<?> waList && !waList.isEmpty()) {
                                Object wa = waList.get(0);
                                if (wa instanceof Map<?, ?> waMap) {
                                    Object authorRef = waMap.get("author");
                                    if (authorRef instanceof Map<?, ?> authorRefMap) {
                                        Object aKey = authorRefMap.get("key");
                                        if (aKey instanceof String authorKey) {
                                            String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                                            ResponseEntity<Map> authorResp = restTemplate.getForEntity(authorUrl, Map.class);
                                            if (authorResp.getBody() != null) {
                                                Object name = authorResp.getBody().get("name");
                                                if (name instanceof String) return (String) name;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Author lookup failed, fall through
        }
        return "Onbekende auteur";
    }

    private LocalDate tryParsePublishDate(String value) {
        String[] patterns = { "yyyy-MM-dd", "yyyy", "MMMM d, yyyy", "MMM d, yyyy" };
        for (String pattern : patterns) {
            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                return LocalDate.parse(value, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }
}
