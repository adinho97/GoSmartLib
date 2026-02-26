package com.example.demo;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemRepository itemRepository;

    @Test
    void getAllShouldReturnItems() throws Exception {
        Item item = new Item();
        item.setId(1L);
        item.setName("Boekenkast");

        when(itemRepository.findAll()).thenReturn(List.of(item));

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Boekenkast"));
    }

    @Test
    void createShouldPersistAndReturnItemWhenPayloadIsValid() throws Exception {
        Item saved = new Item();
        saved.setId(2L);
        saved.setName("Leeslamp");

        when(itemRepository.save(any(Item.class))).thenReturn(saved);

        String json = """
                {
                  "name": "Leeslamp"
                }
                """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2))
                .andExpect(jsonPath("$.name").value("Leeslamp"));
    }

    @Test
    void createShouldReturnBadRequestWhenNameIsBlank() throws Exception {
        String invalidJson = """
                {
                  "name": ""
                }
                """;

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }
}
