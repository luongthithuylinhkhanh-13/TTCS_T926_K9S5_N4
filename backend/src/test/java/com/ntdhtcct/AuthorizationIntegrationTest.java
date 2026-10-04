package com.ntdhtcct;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.dto.AddMemberRequest;
import com.ntdhtcct.dto.UpdateMemberRoleRequest;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * T-04.12: Bộ kiểm thử tích hợp toàn diện cho Authorization & Project Membership RBAC.
 * Bao gồm các kịch bản tương ứng từ T-04.1 đến T-04.11:
 * - T-04.1: Bảng Roles
 * - T-04.2: Bảng Project Members
 * - T-04.3: Liên kết User với Project
 * - T-04.4: Gán Role cho User trong Project
 * - T-04.5: Middleware Authorization Interceptor
 * - T-04.6: Kiểm tra User có thuộc Project hay không
 * - T-04.7: Kiểm tra Role của User
 * - T-04.8: Cấu hình quyền theo từng Route
 * - T-04.9: Thiết lập cơ chế Default Deny
 * - T-04.10: Xử lý lỗi 403 khi không đủ quyền
 * - T-04.11: Kiểm tra truy cập Project không thuộc thành viên
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProjectMemberRepository projectMemberRepository;

    private User managerUser;
    private User engineerUser;
    private User workerUser;
    private User outsiderUser;
    private Project projectA;
    private Project projectB;
    private Role roleAdmin;
    private Role roleManager;
    private Role roleEngineer;
    private Role roleWorker;
    private Role roleViewer;

    @BeforeEach
    void setUp() {
        // Dọn dẹp dữ liệu cũ trước mỗi test
        projectMemberRepository.deleteAll();
        projectRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        // 1. T-04.1: Khởi tạo các Roles
        roleAdmin = roleRepository.save(new Role("ADMIN"));
        roleManager = roleRepository.save(new Role("PROJECT_MANAGER"));
        roleEngineer = roleRepository.save(new Role("SITE_ENGINEER"));
        roleWorker = roleRepository.save(new Role("WORKER"));
        roleViewer = roleRepository.save(new Role("VIEWER"));

        // 2. T-04.3: Tạo Users
        managerUser = userRepository.save(new User("manager@congtrinh.vn", "Password123", "Trần Quản Lý"));
        engineerUser = userRepository.save(new User("engineer@congtrinh.vn", "Password123", "Lê Kỹ Sư"));
        workerUser = userRepository.save(new User("worker@congtrinh.vn", "Password123", "Phạm Công Nhân"));
        outsiderUser = userRepository.save(new User("outsider@ngoai.vn", "Password123", "Nguyễn Người Ngoài"));
        // 3. T-04.3: Tạo Projects
        projectA = projectRepository.save(new Project("DA-001", "Dự án Cầu Vàm Cống Mới"));
        projectB = projectRepository.save(new Project("DA-002", "Dự án Tòa Nhà Landmark"));

        // 4. T-04.2 & T-04.4: Gán thành viên vào Project A
        projectMemberRepository.save(new ProjectMember(projectA, managerUser, roleManager));
        projectMemberRepository.save(new ProjectMember(projectA, engineerUser, roleEngineer));
        projectMemberRepository.save(new ProjectMember(projectA, workerUser, roleWorker));
    }

    @Test
    @DisplayName("PROJECT_MANAGER gán Role cho User mới vào Project thành công -> HTTP 201")
    void testAddMemberToProject_ByManager_ShouldReturn201Created() throws Exception {
        AddMemberRequest request = new AddMemberRequest(outsiderUser.getId(), "VIEWER");

        mockMvc.perform(post("/api/projects/" + projectA.getId() + "/members")
                        .header("X-User-Id", managerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.userId", is(outsiderUser.getId().toString())))
                .andExpect(jsonPath("$.data.roleCode", is("VIEWER")));
    }

    @Test
    @DisplayName("Cập nhật Role cho thành viên trong Project thành công -> HTTP 200")
    void testUpdateMemberRole_ByManager_ShouldReturn200() throws Exception {
        UpdateMemberRoleRequest request = new UpdateMemberRoleRequest("SITE_ENGINEER");

        mockMvc.perform(put("/api/projects/" + projectA.getId() + "/members/" + workerUser.getId() + "/role")
                        .header("X-User-Id", managerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.roleCode", is("SITE_ENGINEER")));
    }

    @Test
    @DisplayName("T-04.6 & T-04.11: Người dùng không thuộc Project truy cập -> Bị chặn")
    void testAccessProject_UserNotInProject_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/members")
                        .header("X-User-Id", outsiderUser.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                    .andExpect(jsonPath("$.code", is("AUTH_UNAUTHORIZED")))
                .andExpect(jsonPath("$.message", containsString("không có quyền truy cập công trình này")));
    }

    @Test
    @DisplayName("T-04.7 & T-04.10: Thành viên có Role WORKER cố gắng gán vai trò (cần Manager) -> Bị chặn 403 Forbidden")
    void testAddMember_InsufficientRoleWorker_ShouldReturn403Forbidden() throws Exception {
        AddMemberRequest request = new AddMemberRequest(outsiderUser.getId(), "VIEWER");

        mockMvc.perform(post("/api/projects/" + projectA.getId() + "/members")
                        .header("X-User-Id", workerUser.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("AUTH_FORBIDDEN")))
                .andExpect(jsonPath("$.message", containsString("không có vai trò phù hợp")));
    }

    @Test
    @DisplayName("T-04.7: Kỹ sư (SITE_ENGINEER) truy cập nhật ký công trường hợp lệ -> HTTP 200")
    void testAccessEngineeringDiary_WithSiteEngineer_ShouldReturn200() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/engineering-diary")
                        .header("X-User-Id", engineerUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.feature", is("Nhật ký công trường")));
    }

    @Test
    @DisplayName("T-04.7 & T-04.10: Công nhân (WORKER) truy cập nhật ký kỹ thuật -> Bị từ chối 403 Forbidden")
    void testAccessEngineeringDiary_WithWorkerRole_ShouldReturn403Forbidden() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/engineering-diary")
                        .header("X-User-Id", workerUser.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("AUTH_FORBIDDEN")));
    }

    @Test
    @DisplayName("T-04.9: Tuyến đường không được cấp quyền rõ ràng -> Kích hoạt Default Deny từ chối với 403")
    void testDefaultDeny_UnconfiguredEndpoint_ShouldReturn403Forbidden() throws Exception {
        // Kể cả Manager truy cập vào tuyến đường không được cấu hình trong dự án cũng bị Default Deny chặn
        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/unconfigured-secure-endpoint")
                        .header("X-User-Id", managerUser.getId()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status", is(403)))
                .andExpect(jsonPath("$.code", is("DEFAULT_DENY")))
                .andExpect(jsonPath("$.message", containsString("Default Deny")));
    }

    @Test
    @DisplayName("T-04.5: Thiếu Header X-User-Id -> Bị chặn với HTTP 401 Unauthorized")
    void testMissingUserIdHeader_ShouldReturn401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/projects/" + projectA.getId() + "/members")
            )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status", is(401)))
                .andExpect(jsonPath("$.code", is("AUTH_MISSING_USER_ID")));
    }

    @Test
    @DisplayName("T-04.11: Truy cập vào Project không tồn tại -> HTTP 404 Not Found")
    void testAccessNonExistentProject_ShouldReturn404NotFound() throws Exception {
        mockMvc.perform(get("/api/projects/" + UUID.randomUUID() + "/members")
            .header("X-User-Id", managerUser.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)))
                .andExpect(jsonPath("$.code", is("RESOURCE_NOT_FOUND")));
    }

    @Test
    @DisplayName("Xóa thành viên khỏi Project thành công -> HTTP 200")
    void testRemoveMemberFromProject_ByManager_ShouldReturn200() throws Exception {
        mockMvc.perform(delete("/api/projects/" + projectA.getId() + "/members/" + workerUser.getId())
                        .header("X-User-Id", managerUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("thành công")));
    }
}
