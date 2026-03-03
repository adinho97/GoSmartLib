package com.example.demo;

import com.example.demo.entities.Boek;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BoekController.class)
class BoekControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BoekRepository boekRepository;

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
        Boek saved = new Boek();
        saved.setId(7L);
        saved.setTitel("Clean Code");
        saved.setAuteur("Robert C. Martin");
        saved.setCover("data:image/png;base64,abc");
        saved.setBeschrijving("Software craftsmanship");
        saved.setGenre("Programming");
        saved.setUitgaveDatum(LocalDate.of(2008, 8, 1));
        saved.setPaginas(464);
        saved.setTaal("English");
        saved.setUitgeverij("Prentice Hall");

        when(boekRepository.save(any(Boek.class))).thenReturn(saved);

        String json = """
                {
                  "titel": "Clean Code",
                  "auteur": "Robert C. Martin",
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
                .andExpect(jsonPath("$.auteur").value("Robert C. Martin"));
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
}
