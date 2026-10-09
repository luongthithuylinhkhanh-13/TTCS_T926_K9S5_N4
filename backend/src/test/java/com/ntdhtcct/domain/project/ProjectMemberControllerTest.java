package com.ntdhtcct.domain.project;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.domain.auth.AuthTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
public class ProjectMemberControllerTest {

    private MockMvc mockMvc;

    private ObjectMapper objectMapper;

    @Mock
    private ProjectMemberService projectMemberService;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private ProjectMemberController projectMemberController;

    private UUID projectId;
    private UUID userId;
    private String token;
    private String validAuthHeader;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders.standaloneSetup(projectMemberController).build();

        projectId = UUID.randomUUID();
        userId = UUID.randomUUID();
        token = "valid-test-token";
        validAuthHeader = "Bearer " + token;

        when(authTokenService.getUserIdFromToken(token)).thenReturn(userId);
    }

    @Test
    void addMember_Unauthorized() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        request.put("role", "CAPTAIN");

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void addMember_MissingEmailOrRole() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "  ");

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", validAuthHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Email and role are required"));
    }

    @Test
    void addMember_Success() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        request.put("role", "CAPTAIN");

        doNothing().when(projectMemberService).addMemberOrInvite(eq(projectId), eq("test@example.com"), eq("CAPTAIN"));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", validAuthHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void addMember_ServiceThrowsException() throws Exception {
        Map<String, String> request = new HashMap<>();
        request.put("email", "test@example.com");
        request.put("role", "CAPTAIN");

        doThrow(new IllegalArgumentException("User is already a member"))
                .when(projectMemberService).addMemberOrInvite(eq(projectId), eq("test@example.com"), eq("CAPTAIN"));

        mockMvc.perform(post("/api/projects/{projectId}/members", projectId)
                .header("Authorization", validAuthHeader)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("User is already a member"));
    }

    @Test
    void removeMember_Success() throws Exception {
        UUID memberIdToRemove = UUID.randomUUID();

        doNothing().when(projectMemberService).removeMember(projectId, memberIdToRemove);

        mockMvc.perform(delete("/api/projects/{projectId}/members/{memberId}", projectId, memberIdToRemove)
                .header("Authorization", validAuthHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
