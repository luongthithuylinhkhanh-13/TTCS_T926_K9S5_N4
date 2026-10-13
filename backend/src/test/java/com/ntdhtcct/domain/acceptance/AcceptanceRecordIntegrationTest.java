package com.ntdhtcct.domain.acceptance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordRequest;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AcceptanceRecordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AcceptanceRecordRepository recordRepository;

    @Autowired
    private AcceptanceRecordAuditLogRepository auditLogRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthTokenService authTokenService;

    private Project project;
    private User manager;
    private User engineer;
    private User viewer;

    @BeforeEach
    void setUp() {
        auditLogRepository.deleteAll();
        recordRepository.deleteAll();
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role managerRole = roleRepository.save(new Role("PROJECT_MANAGER"));
        Role engineerRole = roleRepository.save(new Role("SITE_ENGINEER"));
        Role viewerRole = roleRepository.save(new Role("VIEWER"));
        manager = userRepository.save(new User("manager@acceptance.test", "Password123", "Nguyễn Quản Lý"));
        engineer = userRepository.save(new User("engineer@acceptance.test", "Password123", "Trần Kỹ Sư"));
        viewer = userRepository.save(new User("viewer@acceptance.test", "Password123", "Lê Khách Hàng"));
        project = projectRepository.save(new Project("AC-01", "Công trình kiểm thử nghiệm thu"));
        projectMemberRepository.save(new ProjectMember(project, manager, managerRole));
        projectMemberRepository.save(new ProjectMember(project, engineer, engineerRole));
        projectMemberRepository.save(new ProjectMember(project, viewer, viewerRole));
    }

    @Test
    void logsCreateEditAndApprovalWithActorAndTimestamp() throws Exception {
        String recordId = createRecord(engineer);
        String updateBody = objectMapper.writeValueAsString(recordRequest("Phiếu nghiệm thu đã sửa"));

        mockMvc.perform(put(recordUrl(recordId))
                        .header("Authorization", token(engineer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")));

        mockMvc.perform(post(recordUrl(recordId) + "/approve")
                        .header("Authorization", token(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")));

        mockMvc.perform(get(recordUrl(recordId) + "/history")
                        .header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[0].action", is("CREATE")))
                .andExpect(jsonPath("$[0].actorName", is("Trần Kỹ Sư")))
                .andExpect(jsonPath("$[0].performedAt").exists())
                .andExpect(jsonPath("$[1].action", is("EDIT")))
                .andExpect(jsonPath("$[1].actorName", is("Trần Kỹ Sư")))
                .andExpect(jsonPath("$[2].action", is("APPROVE")))
                .andExpect(jsonPath("$[2].actorName", is("Nguyễn Quản Lý")));

        mockMvc.perform(put(recordUrl(recordId))
                        .header("Authorization", token(engineer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    void logsReturnReasonAndReopensRecordForEditing() throws Exception {
        String recordId = createRecord(manager);

        mockMvc.perform(post(recordUrl(recordId) + "/return")
                        .header("Authorization", token(manager))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"Bổ sung biên bản đo kiểm\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RETURNED")));

        mockMvc.perform(put(recordUrl(recordId))
                        .header("Authorization", token(engineer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordRequest("Phiếu đã bổ sung"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")));

        mockMvc.perform(get(recordUrl(recordId) + "/history")
                        .header("Authorization", token(viewer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].action", is("RETURN")))
                .andExpect(jsonPath("$[1].comment", is("Bổ sung biên bản đo kiểm")))
                .andExpect(jsonPath("$[2].action", is("EDIT")));
    }

    @Test
    void onlyProjectManagersCanApprove() throws Exception {
        String recordId = createRecord(engineer);

        mockMvc.perform(post(recordUrl(recordId) + "/approve")
                        .header("Authorization", token(viewer)))
                .andExpect(status().isForbidden());

        org.junit.jupiter.api.Assertions.assertEquals(1, auditLogRepository.count());
    }

    private String createRecord(User actor) throws Exception {
        String response = mockMvc.perform(post("/api/projects/" + project.getId() + "/acceptance-records")
                        .header("Authorization", token(actor))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(recordRequest("Phiếu nghiệm thu phần móng"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDING_APPROVAL")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).get("id").asText();
    }

    private AcceptanceRecordRequest recordRequest(String title) {
        return new AcceptanceRecordRequest(
                title,
                LocalDate.of(2026, 10, 13),
                "Kiểm tra khối lượng và chất lượng thi công"
        );
    }

    private String recordUrl(String recordId) {
        return "/api/projects/" + project.getId() + "/acceptance-records/" + recordId;
    }

    private String token(User user) {
        return "Bearer " + authTokenService.createToken(user.getId()).getToken();
    }
}
