package com.example.demo.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.demo.repositories.BookRepository;
import com.example.demo.dto.BookDto;
import com.example.demo.dto.ImportResultDto;
import com.example.demo.entities.Book;
import com.example.demo.entities.School;
import com.example.demo.mappers.BookMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class BookService {

    private static final String LANGUAGE_MAPPING_RESOURCE = "language-mapping.json";
    private static final Map<String, String> LANGUAGE_TRANSLATIONS = loadLanguageTranslations();

    private static final String GENRE_MAPPING_RESOURCE = "genre-mapping.json";
    private static final Map<String, String> GENRE_TRANSLATIONS = loadJsonMapping(GENRE_MAPPING_RESOURCE);

    private final BookRepository bookRepository;
    private final SchoolService schoolService;

    public BookService(BookRepository bookRepository, SchoolService schoolService) {
        this.bookRepository = bookRepository;
        this.schoolService = schoolService;
    }

    public Optional<BookDto> findByIsbn(String isbn, Long schoolId) {
        if (schoolId == null) {
            return bookRepository.findByIsbn(isbn)
                    .map(BookMapper::toDto);
        }

        return bookRepository.findByIsbnAndSchool_Id(isbn, schoolId)
                .map(BookMapper::toDto);
    }

    public BookDto fetchPreviewByIsbn(String isbn) {
        Book fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }
        return BookMapper.toDto(fetched);
    }

    @Transactional
    public BookDto importByIsbn(String isbn, Long schoolId) {
        School school = schoolService.getByIdOrDefault(schoolId);

        Optional<Book> existing = bookRepository.findByIsbnAndSchool_Id(isbn, school.getId());
        if (existing.isPresent()) {
            return BookMapper.toDto(existing.get());
        }

        Book fetched = fetchBookFromOpenLibrary(isbn);
        if (fetched == null) {
            return null;
        }

        fetched.setSchool(school);
        Book saved = bookRepository.save(fetched);
        return BookMapper.toDto(saved);
    }

    public ImportResultDto importBulkByIsbn(MultipartFile file, Long schoolId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }

        // Validate school early to keep behavior consistent with single ISBN import.
        schoolService.getByIdOrDefault(schoolId);

        ImportResultDto result = new ImportResultDto();
        result.setTotalRows(0);
        result.setUniqueIsbnsProcessed(0);
        result.setDuplicateRowsSkipped(0);
        return result;
    }

    private Book fetchBookFromOpenLibrary(String isbn) {
        String url = "https://openlibrary.org/isbn/" + isbn + ".json";
        RestTemplate restTemplate = new RestTemplate();

        try {
            Map<String, Object> body = fetchJsonMap(url, restTemplate);
            if (body == null) {
                return null;
            }

            Book book = new Book();
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

            book.setTaal(resolveLanguage(body));

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

            // Genre: derived from subjects on edition or work
            book.setGenre(resolveGenre(body, restTemplate));

            return book;
        } catch (HttpClientErrorException.NotFound e) {
            // ISBN not found in OpenLibrary
            return null;
        } catch (Exception e) {
            // Network or parsing error etc.
            return null;
        }
    }

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
                        Map<String, Object> authorBody = fetchJsonMap(authorUrl, restTemplate);
                        if (authorBody != null) {
                            Object name = authorBody.get("name");
                            if (name instanceof String) {
                                return (String) name;
                            }
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
                        Map<String, Object> workBody = fetchJsonMap(workUrl, restTemplate);
                        if (workBody != null) {
                            Object workAuthors = workBody.get("authors");
                            if (workAuthors instanceof List<?> waList && !waList.isEmpty()) {
                                Object wa = waList.get(0);
                                if (wa instanceof Map<?, ?> waMap) {
                                    Object authorRef = waMap.get("author");
                                    if (authorRef instanceof Map<?, ?> authorRefMap) {
                                        Object aKey = authorRefMap.get("key");
                                        if (aKey instanceof String authorKey) {
                                            String authorUrl = "https://openlibrary.org" + authorKey + ".json";
                                            Map<String, Object> authorBody = fetchJsonMap(authorUrl, restTemplate);
                                            if (authorBody != null) {
                                                Object name = authorBody.get("name");
                                                if (name instanceof String) {
                                                    return (String) name;
                                                }
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

    private String resolveGenre(Map<String, Object> body, RestTemplate restTemplate) {
        List<String> subjects = extractSubjects(body);

        if (subjects.isEmpty()) {
            // Fall back to subjects on the linked work
            try {
                Object works = body.get("works");
                if (works instanceof List<?> workList && !workList.isEmpty()) {
                    Object firstWork = workList.get(0);
                    if (firstWork instanceof Map<?, ?> workMap) {
                        Object key = workMap.get("key");
                        if (key instanceof String workKey) {
                            String workUrl = "https://openlibrary.org" + workKey + ".json";
                            Map<String, Object> workBody = fetchJsonMap(workUrl, restTemplate);
                            if (workBody != null) {
                                subjects = extractSubjects(workBody);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (subjects.isEmpty())
            return null;

        String genre = subjects.stream()
                .limit(3)
                .map(this::translateSubject)
                .collect(Collectors.joining(", "));
        return genre.length() > 100 ? genre.substring(0, 100) : genre;
    }

    private List<String> extractSubjects(Map<?, ?> body) {
        Object subjects = body.get("subjects");
        if (subjects instanceof List<?> list) {
            return list.stream()
                    .filter(s -> s instanceof String)
                    .map(s -> (String) s)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    private Map<String, Object> fetchJsonMap(String url, RestTemplate restTemplate) {
        ResponseEntity<Object> response = restTemplate.getForEntity(url, Object.class);
        if (!response.getStatusCode().is2xxSuccessful()) {
            return null;
        }

        Object responseBody = response.getBody();
        if (!(responseBody instanceof Map<?, ?> rawMap)) {
            return null;
        }

        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawMap.entrySet()) {
            if (entry.getKey() instanceof String key) {
                result.put(key, entry.getValue());
            }
        }
        return result;
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

    private String resolveLanguage(Map<String, Object> body) {
        Object languages = body.get("languages");
        if (languages instanceof List<?> languageList && !languageList.isEmpty()) {
            Object first = languageList.get(0);
            if (first instanceof Map<?, ?> langMap) {
                Object key = langMap.get("key");
                if (key instanceof String langKey) {
                    return mapLanguageCodeToDutch(langKey);
                }
            } else if (first instanceof String langCode) {
                return mapLanguageCodeToDutch(langCode);
            }
        }

        // Fallbacks sometimes present in third-party payloads
        Object language = body.get("language");
        if (language instanceof String lang) {
            return mapLanguageCodeToDutch(lang);
        }

        return "Onbekend";
    }

    private String mapLanguageCodeToDutch(String rawCode) {
        if (rawCode == null)
            return "Onbekend";
        String code = rawCode.trim().toLowerCase(Locale.ROOT);
        int slash = code.lastIndexOf('/');
        if (slash >= 0 && slash + 1 < code.length())
            code = code.substring(slash + 1);
        return code.isEmpty() ? "Onbekend" : LANGUAGE_TRANSLATIONS.getOrDefault(code, "Onbekend");
    }

    private String translateSubject(String subject) {
        if (subject == null)
            return "";
        String key = subject.trim().toLowerCase(Locale.ROOT);
        return GENRE_TRANSLATIONS.getOrDefault(key, subject);
    }

    private static Map<String, String> loadLanguageTranslations() {
        return loadJsonMapping(LANGUAGE_MAPPING_RESOURCE);
    }

    private static Map<String, String> loadJsonMapping(String resource) {
        try (InputStream in = BookService.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null)
                return Collections.emptyMap();
            Map<String, String> raw = new ObjectMapper().readValue(in, new TypeReference<>() {
            });
            Map<String, String> result = new HashMap<>();
            raw.forEach((k, v) -> {
                if (k != null && !k.isBlank())
                    result.put(k.trim().toLowerCase(Locale.ROOT), v != null ? v : "");
            });
            return Collections.unmodifiableMap(result);
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }
}
