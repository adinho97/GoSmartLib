package com.example.demo;

import com.example.demo.config.ConnectionPoolMonitor;
import com.example.demo.config.CustomAccessDeniedHandler;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.controllers.BibbeheerderController;
import com.example.demo.dto.admin.user.AdminUserListItem;
import com.example.demo.exception.ApiException;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.security.SecurityConfig;
import com.example.demo.services.BibbeheerderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BibbeheerderController.class)
@Import({SecurityConfig.class, CustomAccessDeniedHandler.class})
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost")
class BibbeheerderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BibbeheerderService bibbeheerderService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SuperAdminRepository superAdminRepository;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private ConnectionPoolMonitor connectionPoolMonitor;

    // --- GET /api/bibbeheerder/leerkrachten ---

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void getLeerkrachten_shouldReturnListForBibbeheerder() throws Exception {
        AdminUserListItem item = makeItem(10L, "leerkracht-sub", "leerkracht");

        when(bibbeheerderService.getLeerkrachtenInOwnSchool("bib-sub"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].sub").value("leerkracht-sub"))
                .andExpect(jsonPath("$[0].role").value("leerkracht"));
    }

    @Test
    @WithMockUser(username = "admin-sub", roles = "SUPER_ADMIN")
    void getLeerkrachten_shouldReturnListForSuperAdmin() throws Exception {
        AdminUserListItem item = makeItem(11L, "teacher-sub", "leerkracht");

        when(bibbeheerderService.getLeerkrachtenInOwnSchool("admin-sub"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(11));
    }

    @Test
    @WithMockUser(roles = "LEERKRACHT")
    void getLeerkrachten_shouldReturn403ForLeerkracht() throws Exception {
        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LEERLING")
    void getLeerkrachten_shouldReturn403ForLeerling() throws Exception {
        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getLeerkrachten_shouldReturn403ForUnauthenticated() throws Exception {
        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void getLeerkrachten_shouldReturnEmptyListWhenNoLeerkrachten() throws Exception {
        when(bibbeheerderService.getLeerkrachtenInOwnSchool("bib-sub"))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/bibbeheerder/leerkrachten"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    // --- PATCH /api/bibbeheerder/leerkrachten/{userId}/promote ---

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void promoteLeerkracht_shouldReturn200ForBibbeheerder() throws Exception {
        AdminUserListItem promoted = makeItem(20L, "target-sub", "bibbeheerder");

        when(bibbeheerderService.promoteLeerkrachtToBibbeheerder("bib-sub", 20L))
                .thenReturn(promoted);

        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.sub").value("target-sub"))
                .andExpect(jsonPath("$.role").value("bibbeheerder"));
    }

    @Test
    @WithMockUser(username = "admin-sub", roles = "SUPER_ADMIN")
    void promoteLeerkracht_shouldReturn200ForSuperAdmin() throws Exception {
        AdminUserListItem promoted = makeItem(21L, "target-sub-2", "bibbeheerder");

        when(bibbeheerderService.promoteLeerkrachtToBibbeheerder("admin-sub", 21L))
                .thenReturn(promoted);

        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/21/promote"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(21));
    }

    @Test
    @WithMockUser(roles = "LEERKRACHT")
    void promoteLeerkracht_shouldReturn403ForLeerkracht() throws Exception {
        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "LEERLING")
    void promoteLeerkracht_shouldReturn403ForLeerling() throws Exception {
        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isForbidden());
    }

    @Test
    void promoteLeerkracht_shouldReturn403ForUnauthenticated() throws Exception {
        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void promoteLeerkracht_shouldReturn404WhenUserNotFound() throws Exception {
        when(bibbeheerderService.promoteLeerkrachtToBibbeheerder(any(), eq(99L)))
                .thenThrow(new ApiException("Gebruiker niet gevonden", HttpStatus.NOT_FOUND, "USER_NOT_FOUND"));

        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/99/promote"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void promoteLeerkracht_shouldReturn400WhenInvalidRoleTransition() throws Exception {
        when(bibbeheerderService.promoteLeerkrachtToBibbeheerder(any(), eq(20L)))
                .thenThrow(new ApiException("Alleen leerkrachten kunnen worden gepromoveerd", HttpStatus.BAD_REQUEST, "INVALID_ROLE_TRANSITION"));

        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "bib-sub", roles = "BIBBEHEERDER")
    void promoteLeerkracht_shouldReturn403WhenCrossSchoolAccess() throws Exception {
        when(bibbeheerderService.promoteLeerkrachtToBibbeheerder(any(), eq(20L)))
                .thenThrow(new ApiException("Gebruiker behoort niet tot uw school", HttpStatus.FORBIDDEN, "ACCESS_DENIED"));

        mockMvc.perform(patch("/api/bibbeheerder/leerkrachten/20/promote"))
                .andExpect(status().isForbidden());
    }

    // --- helpers ---

    private AdminUserListItem makeItem(Long id, String sub, String role) {
        AdminUserListItem item = new AdminUserListItem();
        item.setId(id);
        item.setSub(sub);
        item.setRole(role);
        item.setActive(true);
        return item;
    }
}
