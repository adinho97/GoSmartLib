package com.example.demo;

import com.example.demo.config.ConnectionPoolMonitor;
import com.example.demo.config.JwtTokenProvider;
import com.example.demo.controllers.LoanController;
import com.example.demo.dto.LoanDto;
import com.example.demo.repositories.AppUserRepository;
import com.example.demo.repositories.SuperAdminRepository;
import com.example.demo.security.SecurityConfig;
import com.example.demo.services.LoanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LoanController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = "app.cors.allowed-origins=http://localhost")
class LoanControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LoanService loanService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private SuperAdminRepository superAdminRepository;

    @MockBean
    private AppUserRepository appUserRepository;

    @MockBean
    private ConnectionPoolMonitor connectionPoolMonitor;

    @Test
    @WithMockUser(roles = "BIBBEHEERDER")
    void getAllActiveLoansShouldReturnLoansForLibrarian() throws Exception {
        LoanDto dto = new LoanDto();
        dto.setId(7L);
        dto.setCopyId(31L);
        dto.setBookId(3L);
        dto.setBookTitel("Dune");
        dto.setBookCover("cover");
        dto.setUserSub("student-1");
        dto.setLoanedAt(LocalDate.of(2026, 4, 10));
        dto.setDueDate(LocalDate.of(2026, 4, 24));
        dto.setReturnedAt(null);

        when(loanService.getAllActiveLoans()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/uitleningen/all-active"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].copyId").value(31))
                .andExpect(jsonPath("$[0].bookId").value(3))
                .andExpect(jsonPath("$[0].bookTitel").value("Dune"))
                .andExpect(jsonPath("$[0].userSub").value("student-1"));
    }

    @Test
    @WithMockUser(roles = "LEERKRACHT")
    void getAllActiveLoansShouldRejectNonLibrarianUsers() throws Exception {
        mockMvc.perform(get("/api/uitleningen/all-active"))
                .andExpect(status().isForbidden());
    }
}
