package com.example.demo;

import com.example.demo.controllers.LoanController;
import com.example.demo.dto.LoanDto;
import com.example.demo.entities.BookCopy;
import com.example.demo.services.LoanService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LoanControllerTest {

    private MockMvc mockMvc;
    private LoanService loanService;

    @BeforeEach
    void setUp() {
        loanService = mock(LoanService.class);
        LoanController controller = new LoanController(loanService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
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

        mockMvc.perform(get("/api/uitleningen/all-active").header("X-User-Role", "bibbeheerder"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(7))
                .andExpect(jsonPath("$[0].copyId").value(31))
                .andExpect(jsonPath("$[0].bookId").value(3))
                .andExpect(jsonPath("$[0].bookTitel").value("Dune"))
                .andExpect(jsonPath("$[0].userSub").value("student-1"));
    }

    @Test
    void getAllActiveLoansShouldRejectNonLibrarianUsers() throws Exception {
        mockMvc.perform(get("/api/uitleningen/all-active").header("X-User-Role", "leerkracht"))
                .andExpect(status().isForbidden());
    }
}