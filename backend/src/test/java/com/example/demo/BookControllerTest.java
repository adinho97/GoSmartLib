package com.example.demo;

import com.example.demo.dto.BoekDto;
import com.example.demo.entities.Boek;
import com.example.demo.entities.School;
import com.example.demo.services.BoekService;
import com.example.demo.services.SchoolService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoekController.class)
@AutoConfigureMockMvc(addFilters = false)
class BookControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockBean
        private BoekRepository boekRepository;

        @MockBean
        private BoekService boekService;

        @MockBean
        private SchoolService schoolService;

        @Test
        void getAllShouldReturnBooks() throws Exception {
                Boek boek = new Boek();
                boek.setId(1L);
                boek.setTitel("Dune");
                boek.setAuteur("Frank Herbert");
                boek.setGenre("Sciencefiction");

                when(boekRepository.findAll()).thenReturn(List.of(boek));

                mockMvc.perform(get("/api/boeken"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0].id").value(1))
                                .andExpect(jsonPath("$[0].titel").value("Dune"))
                                .andExpect(jsonPath("$[0].auteur").value("Frank Herbert"));
        }

        @Test
        void createShouldPersistAndReturnBookWhenPayloadIsValid() throws Exception {
                School school = new School();
                school.setId(1L);
                school.setNaam("GO! Atheneum Antwerpen");

                Boek saved = new Boek();
                saved.setId(7L);
                saved.setTitel("Clean Code");
                saved.setAuteur("Robert C. Martin");
                saved.setCover("data:image/png;base64,abc");
                saved.setBeschrijving("Software craftsmanship");
                saved.setGenre("Programming");
                saved.setIsbn("9780132350884");
                saved.setUitgaveDatum(LocalDate.of(2008, 8, 1));
                saved.setPaginas(464);
                saved.setTaal("English");
                saved.setUitgeverij("Prentice Hall");

                when(schoolService.getByIdOrDefault(any())).thenReturn(school);
                when(boekRepository.findByIsbnAndSchool_Id("9780132350884", 1L)).thenReturn(Optional.empty());
                when(boekRepository.save(any(Boek.class))).thenReturn(saved);

                String json = """
                                {
                                  "titel": "Clean Code",
                                  "auteur": "Robert C. Martin",
                                  "isbn": "9780132350884",
                                  "cover": "data:image/png;base64,abc",
                                  "beschrijving": "Software craftsmanship",
                                  "genre": "Programming",
                                  "uitgaveDatum": "2008-08-01",
                                  "paginas": 464,
                                  "taal": "English",
                                  "uitgeverij": "Prentice Hall"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(7))
                                .andExpect(jsonPath("$.titel").value("Clean Code"))
                                .andExpect(jsonPath("$.auteur").value("Robert C. Martin"))
                                .andExpect(jsonPath("$.isbn").value("9780132350884"));
        }

        @Test
        void createShouldReturnConflictWhenIsbnAlreadyExists() throws Exception {
                School school = new School();
                school.setId(1L);

                Boek existing = new Boek();
                existing.setId(5L);
                existing.setTitel("Existing Book");
                existing.setAuteur("Existing Author");
                existing.setIsbn("9780132350884");

                when(schoolService.getByIdOrDefault(any())).thenReturn(school);
                when(boekRepository.findByIsbnAndSchool_Id("9780132350884", 1L)).thenReturn(Optional.of(existing));

                String json = """
                                {
                                  "titel": "Clean Code",
                                  "auteur": "Robert C. Martin",
                                  "isbn": "9780132350884"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json))
                                .andExpect(status().isConflict());
        }

        @Test
        void createShouldReturnBadRequestWhenRequiredFieldIsMissing() throws Exception {
                String invalidJson = """
                                {
                                  "titel": "",
                                  "auteur": "Auteur"
                                }
                                """;

                mockMvc.perform(post("/api/boeken")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidJson))
                                .andExpect(status().isBadRequest());
        }

        // ---- GET /api/boeken/isbn/{isbn} ----------------------------------------

        @Test
        void getByIsbnShouldReturnBookWhenFoundInDb() throws Exception {
                when(boekService.findByIsbn("9780553808049", null)).thenReturn(Optional.of(makeDto()));

                mockMvc.perform(get("/api/boeken/isbn/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.auteur").value("Frank Herbert"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void getByIsbnShouldReturn404WhenNotInDb() throws Exception {
                when(boekService.findByIsbn("0000000000000", null)).thenReturn(Optional.empty());

                mockMvc.perform(get("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- GET /api/boeken/preview/{isbn} -------------------------------------

        @Test
        void previewByIsbnShouldReturnBookFromOpenLibrary() throws Exception {
                when(boekService.fetchPreviewByIsbn("9780553808049")).thenReturn(makeDto());

                mockMvc.perform(get("/api/boeken/preview/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void previewByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(boekService.fetchPreviewByIsbn("0000000000000")).thenReturn(null);

                mockMvc.perform(get("/api/boeken/preview/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- POST /api/boeken/isbn/{isbn} ---------------------------------------

        @Test
        void importByIsbnShouldReturnSavedBook() throws Exception {
                when(boekService.importByIsbn("9780553808049", null)).thenReturn(makeDto());

                mockMvc.perform(post("/api/boeken/isbn/9780553808049"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.titel").value("Dune"))
                                .andExpect(jsonPath("$.isbn").value("9780553808049"));
        }

        @Test
        void importByIsbnShouldReturn404WhenNotFoundInOpenLibrary() throws Exception {
                when(boekService.importByIsbn("0000000000000", null)).thenReturn(null);

                mockMvc.perform(post("/api/boeken/isbn/0000000000000"))
                                .andExpect(status().isNotFound());
        }

        // ---- helpers ------------------------------------------------------------

        private BoekDto makeDto() {
                BoekDto dto = new BoekDto();
                dto.setId(1L);
                dto.setTitel("Dune");
                dto.setAuteur("Frank Herbert");
                dto.setIsbn("9780553808049");
                return dto;
        }

        @Test
        void deleteShouldReturnNoContentWhenBookExists() throws Exception {
                when(boekRepository.existsById(1L)).thenReturn(true);
                doNothing().when(boekRepository).deleteById(1L);

                mockMvc.perform(delete("/api/boeken/1"))
                                .andExpect(status().isNoContent());
        }

        @Test
        void deleteShouldReturnNotFoundWhenBookDoesNotExist() throws Exception {
                when(boekRepository.existsById(999L)).thenReturn(false);

                mockMvc.perform(delete("/api/boeken/999"))
                                .andExpect(status().isNotFound());
        }
}
