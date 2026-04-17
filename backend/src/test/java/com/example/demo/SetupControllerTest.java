package com.example.demo;

import com.example.demo.controllers.SetupController;
import com.example.demo.dto.ConfirmInviteResponse;
import com.example.demo.dto.InviteValidationResponse;
import com.example.demo.dto.TeacherDto;
import com.example.demo.services.InviteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SetupController.class)
@AutoConfigureMockMvc(addFilters = false)
class SetupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InviteService inviteService;

    @Test
    void validateInvite_shouldReturnOkAndStoreTokenInSession() throws Exception {
        String token = "valid-token";
        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(true, "Geldig", "aphogeschool"));

        MvcResult result = mockMvc.perform(get("/api/setup/invite/{token}", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.schoolId").value("aphogeschool"))
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertEquals(token, session.getAttribute("inviteToken"));
    }

    @Test
    void validateInvite_shouldReturnGoneWhenInvalid() throws Exception {
        String token = "expired-token";
        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(false, "Deze link is verlopen", null));

        mockMvc.perform(get("/api/setup/invite/{token}", token))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void getTeachers_shouldReturnUnauthorizedWhenSessionTokenMissing() throws Exception {
        mockMvc.perform(get("/api/setup/invite/{token}/teachers", "tkn"))
                .andExpect(status().isUnauthorized());

        verify(inviteService, never()).validateInvite("tkn");
    }

    @Test
    void getTeachers_shouldReturnGoneWhenInviteInvalid() throws Exception {
        String token = "invite-token";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("inviteToken", token);

        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(false, "Deze link is verlopen", null));

        mockMvc.perform(get("/api/setup/invite/{token}/teachers", token)
                        .session(session))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.valid").value(false));
    }

    @Test
    void getTeachers_shouldReturnTeacherListWhenInviteValid() throws Exception {
        String token = "invite-token";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("inviteToken", token);

        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(true, "Geldig", "aphogeschool"));
        when(inviteService.getTeachersBySchool("aphogeschool"))
                .thenReturn(List.of(new TeacherDto(1L, "teacher-sub", "Teacher Name")));

        mockMvc.perform(get("/api/setup/invite/{token}/teachers", token)
                        .session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].sub").value("teacher-sub"));
    }

    @Test
    void confirmInvite_shouldReturnUnauthorizedWhenSessionTokenMissing() throws Exception {
        mockMvc.perform(post("/api/setup/invite/{token}/confirm", "tkn")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"selectedTeacherId\":1}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void confirmInvite_shouldReturnBadRequestWhenConfirmFails() throws Exception {
        String token = "invite-token";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("inviteToken", token);
        session.setAttribute("userSub", "director-sub");

        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(true, "Geldig", "aphogeschool"));
        when(inviteService.confirmInvite(eq(token), eq(2L), eq("director-sub")))
                .thenReturn(new ConfirmInviteResponse(false, "Leerkracht niet gevonden"));

        mockMvc.perform(post("/api/setup/invite/{token}/confirm", token)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"selectedTeacherId\":2}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void confirmInvite_shouldReturnOkAndClearSessionWhenConfirmSucceeds() throws Exception {
        String token = "invite-token";
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("inviteToken", token);
        session.setAttribute("userSub", "director-sub");

        when(inviteService.validateInvite(token))
                .thenReturn(new InviteValidationResponse(true, "Geldig", "aphogeschool"));
        when(inviteService.confirmInvite(eq(token), eq(1L), eq("director-sub")))
                .thenReturn(new ConfirmInviteResponse(true, "Bibbeheerder succesvol aangesteld!"));

        mockMvc.perform(post("/api/setup/invite/{token}/confirm", token)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{" + "\"selectedTeacherId\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        assertNull(session.getAttribute("inviteToken"));
    }

    @Test
    void generateInvite_shouldReturnForbiddenWhenAdminKeyMissingOrInvalid() throws Exception {
        mockMvc.perform(post("/api/setup/generate-invite")
                        .param("schoolId", "aphogeschool")
                        .header("X-Admin-Key", "wrong-key"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("Unauthorized"));

        verify(inviteService, never()).generateInvite("aphogeschool");
    }
}
