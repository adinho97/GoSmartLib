package com.example.demo;

import com.example.demo.controllers.FavoriteController;
import com.example.demo.dto.FavoriteAddRequest;
import com.example.demo.dto.FavoriteDto;
import com.example.demo.entities.AppUser;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.FavoriteRepository;
import com.example.demo.services.FavoriteService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FavoriteController.class)
class FavoriteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FavoriteService favoriteService;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private FavoriteRepository favoriteRepository;

    @Autowired
    private ObjectMapper objectMapper;

    private AppUser createTestUser() {
        AppUser user = new AppUser();
        user.setId(1L);
        user.setSub("test-user-sub");
        user.setRole("leerling");
        return user;
    }

    @Test
    void addToFavorites_ShouldReturnCreated_WhenValidRequest() throws Exception {
        AppUser user = createTestUser();
        FavoriteAddRequest request = new FavoriteAddRequest();
        request.setBookId(1L);

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        doNothing().when(favoriteService).addToFavorites(1L, user);

        mockMvc.perform(post("/api/favorieten")
                .header("X-User-Sub", "test-user-sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(favoriteService).addToFavorites(1L, user);
    }

    @Test
    void addToFavorites_ShouldReturnUnauthorized_WhenNoUserSub() throws Exception {
        FavoriteAddRequest request = new FavoriteAddRequest();
        request.setBookId(1L);

        mockMvc.perform(post("/api/favorieten")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).addToFavorites(any(), any());
    }

    @Test
    void addToFavorites_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        FavoriteAddRequest request = new FavoriteAddRequest();
        request.setBookId(1L);

        when(appUserRepository.findBySub("invalid-sub")).thenReturn(Optional.empty());

        mockMvc.perform(post("/api/favorieten")
                .header("X-User-Sub", "invalid-sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).addToFavorites(any(), any());
    }

    @Test
    void addToFavorites_ShouldReturnBadRequest_WhenServiceThrowsException() throws Exception {
        AppUser user = createTestUser();
        FavoriteAddRequest request = new FavoriteAddRequest();
        request.setBookId(1L);

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        doThrow(new IllegalArgumentException("Boek niet gevonden")).when(favoriteService).addToFavorites(1L, user);

        mockMvc.perform(post("/api/favorieten")
                .header("X-User-Sub", "test-user-sub")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verify(favoriteService).addToFavorites(1L, user);
    }

    @Test
    void removeFromFavorites_ShouldReturnNoContent_WhenValidRequest() throws Exception {
        AppUser user = createTestUser();

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        doNothing().when(favoriteService).removeFromFavorites(1L, user);

        mockMvc.perform(delete("/api/favorieten/1")
                .header("X-User-Sub", "test-user-sub"))
                .andExpect(status().isNoContent());

        verify(favoriteService).removeFromFavorites(1L, user);
    }

    @Test
    void removeFromFavorites_ShouldReturnUnauthorized_WhenNoUserSub() throws Exception {
        mockMvc.perform(delete("/api/favorieten/1"))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).removeFromFavorites(any(), any());
    }

    @Test
    void removeFromFavorites_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        when(appUserRepository.findBySub("invalid-sub")).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/favorieten/1")
                .header("X-User-Sub", "invalid-sub"))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).removeFromFavorites(any(), any());
    }

    @Test
    void getUserFavorites_ShouldReturnFavorites_WhenValidRequest() throws Exception {
        AppUser user = createTestUser();
        FavoriteDto favoriteDto = new FavoriteDto(1L, 1L, "Test Book", "Test Author", "cover.jpg", LocalDateTime.now());
        List<FavoriteDto> favorites = Arrays.asList(favoriteDto);

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        when(favoriteService.getUserFavorites(user)).thenReturn(favorites);

        mockMvc.perform(get("/api/favorieten")
                .header("X-User-Sub", "test-user-sub"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].bookId").value(1))
                .andExpect(jsonPath("$[0].titel").value("Test Book"))
                .andExpect(jsonPath("$[0].auteur").value("Test Author"));

        verify(favoriteService).getUserFavorites(user);
    }

    @Test
    void getUserFavorites_ShouldReturnUnauthorized_WhenNoUserSub() throws Exception {
        mockMvc.perform(get("/api/favorieten"))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).getUserFavorites(any());
    }

    @Test
    void getUserFavorites_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        when(appUserRepository.findBySub("invalid-sub")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/favorieten")
                .header("X-User-Sub", "invalid-sub"))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).getUserFavorites(any());
    }

    @Test
    void isFavorited_ShouldReturnTrue_WhenBookIsFavorited() throws Exception {
        AppUser user = createTestUser();

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        when(favoriteService.isFavorited(1L, user)).thenReturn(true);

        mockMvc.perform(get("/api/favorieten/1/check")
                .header("X-User-Sub", "test-user-sub"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(favoriteService).isFavorited(1L, user);
    }

    @Test
    void isFavorited_ShouldReturnFalse_WhenBookIsNotFavorited() throws Exception {
        AppUser user = createTestUser();

        when(appUserRepository.findBySub("test-user-sub")).thenReturn(Optional.of(user));
        when(favoriteService.isFavorited(1L, user)).thenReturn(false);

        mockMvc.perform(get("/api/favorieten/1/check")
                .header("X-User-Sub", "test-user-sub"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(favoriteService).isFavorited(1L, user);
    }

    @Test
    void isFavorited_ShouldReturnUnauthorized_WhenNoUserSub() throws Exception {
        mockMvc.perform(get("/api/favorieten/1/check"))
                .andExpect(status().isUnauthorized());

        verify(favoriteService, never()).isFavorited(any(), any());
    }

    @Test
    void isFavorited_ShouldReturnBadRequest_WhenUserNotFound() throws Exception {
        when(appUserRepository.findBySub("invalid-sub")).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/favorieten/1/check")
                .header("X-User-Sub", "invalid-sub"))
                .andExpect(status().isBadRequest());

        verify(favoriteService, never()).isFavorited(any(), any());
    }
}