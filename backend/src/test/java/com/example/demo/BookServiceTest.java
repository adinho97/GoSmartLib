package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.entities.School;
import com.example.demo.services.BoekService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

    @Mock
    private BoekRepository boekRepository;

    @InjectMocks
    private BoekService boekService;

    @Mock
    private SchoolService schoolService;

    private School makeSchool() {
        School school = new School();
        school.setId(1L);
        school.setNaam("GO! Atheneum Antwerpen");
        return school;
    }

    private Boek makeBook() {
        Boek b = new Boek();
        b.setId(1L);
        b.setTitel("Dune");
        b.setAuteur("Frank Herbert");
        b.setIsbn("9780553808049");
        return b;
    }

    // ---- findByIsbn ---------------------------------------------------------

    @Test
    void findByIsbnShouldReturnDtoWhenBookIsInDb() {
        when(boekRepository.findByIsbn("9780553808049")).thenReturn(Optional.of(makeBook()));

        Optional<BoekDto> result = boekService.findByIsbn("9780553808049", null);

        assertTrue(result.isPresent());
        assertEquals("Dune", result.get().getTitel());
        assertEquals("Frank Herbert", result.get().getAuteur());
        assertEquals("9780553808049", result.get().getIsbn());
    }

    @Test
    void findByIsbnShouldReturnEmptyWhenNotInDb() {
        when(boekRepository.findByIsbn("0000000000000")).thenReturn(Optional.empty());

        Optional<BoekDto> result = boekService.findByIsbn("0000000000000", null);

        assertFalse(result.isPresent());
    }

    // ---- importByIsbn -------------------------------------------------------

    @Test
    void importByIsbnShouldReturnExistingBookWithoutCallingOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(boekRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.of(makeBook()));

        BoekDto result = boekService.importByIsbn("9780553808049", 1L);

        assertNotNull(result);
        assertEquals("Dune", result.getTitel());
        verify(boekRepository, never()).save(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void importByIsbnShouldFetchFromOpenLibrarySaveAndReturnNewBook() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(boekRepository.findByIsbnAndSchool_Id("9780553808049", 1L)).thenReturn(Optional.empty());
        when(boekRepository.save(any(Boek.class))).thenReturn(makeBook());

        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Dune");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL2732061A")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Frank Herbert");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.getForEntity(contains("isbn"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
            when(rt.getForEntity(contains("authors"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BoekDto result = boekService.importByIsbn("9780553808049", 1L);

            assertNotNull(result);
            assertEquals("Dune", result.getTitel());
            verify(boekRepository).save(any(Boek.class));
        }
    }

    @Test
    void importByIsbnShouldReturnNullWhenIsbnNotFoundInOpenLibrary() {
        when(schoolService.getByIdOrDefault(1L)).thenReturn(makeSchool());
        when(boekRepository.findByIsbnAndSchool_Id("0000000000000", 1L)).thenReturn(Optional.empty());

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (rt, ctx) -> when(rt.getForEntity(anyString(), eq(Map.class)))
                        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)))) {
            BoekDto result = boekService.importByIsbn("0000000000000", 1L);

            assertNull(result);
            verify(boekRepository, never()).save(any());
        }
    }

    // ---- fetchPreviewByIsbn -------------------------------------------------

    @Test
    @SuppressWarnings("unchecked")
    void fetchPreviewByIsbnShouldReturnDtoWithoutPersisting() {
        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Dune");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL2732061A")));
        bookBody.put("languages", List.of(Map.of("key", "/languages/eng")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Frank Herbert");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.getForEntity(contains("isbn"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
            when(rt.getForEntity(contains("authors"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BoekDto result = boekService.fetchPreviewByIsbn("9780553808049");

            assertNotNull(result);
            assertEquals("Dune", result.getTitel());
            assertEquals("Frank Herbert", result.getAuteur());
            assertEquals("Engels", result.getTaal());
            verify(boekRepository, never()).save(any());
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    void fetchPreviewByIsbnShouldMapFrenchLanguageToFrench() {
        Map<String, Object> bookBody = new HashMap<>();
        bookBody.put("title", "Le Petit Prince");
        bookBody.put("authors", List.of(Map.of("key", "/authors/OL1000000A")));
        bookBody.put("languages", List.of(Map.of("key", "/languages/fre")));

        Map<String, Object> authorBody = new HashMap<>();
        authorBody.put("name", "Antoine de Saint-Exupery");

        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class, (rt, ctx) -> {
            when(rt.getForEntity(contains("isbn"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(bookBody));
            when(rt.getForEntity(contains("authors"), eq(Map.class)))
                    .thenReturn(ResponseEntity.ok(authorBody));
        })) {
            BoekDto result = boekService.fetchPreviewByIsbn("9780156012195");

            assertNotNull(result);
            assertEquals("Frans", result.getTaal());
        }
    }

    @Test
    void fetchPreviewByIsbnShouldReturnNullWhenNotFoundInOpenLibrary() {
        try (MockedConstruction<RestTemplate> mocked = mockConstruction(RestTemplate.class,
                (rt, ctx) -> when(rt.getForEntity(anyString(), eq(Map.class)))
                        .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND)))) {
            BoekDto result = boekService.fetchPreviewByIsbn("0000000000000");

            assertNull(result);
        }
    }
}
