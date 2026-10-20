package com.ntdhtcct.controller;

import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.time.OffsetDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private AuthTokenService authTokenService;

    @Mock
    private ProjectRepository projectRepository;

    private AdminUserController controller;

    private User adminUser;
    private User normalUser;
    private UUID adminId;
    private UUID normalUserId;

    @BeforeEach
    void setUp() {
        controller = new AdminUserController(userRepository, roleRepository, authTokenService, projectRepository);

        adminId = UUID.randomUUID();
        adminUser = new User("admin@admin.com", "hash", "Admin");
        adminUser.setId(adminId);
        adminUser.setRole(new Role("ADMIN"));
        adminUser.setStatus("ACTIVE");

        normalUserId = UUID.randomUUID();
        normalUser = new User("user@test.com", "hash", "User");
        normalUser.setId(normalUserId);
        normalUser.setRole(new Role("WORKER"));
        normalUser.setStatus("PENDING");
    }

    @Test
    @DisplayName("Admin lấy thống kê dashboard thành công")
    void getDashboardStats_success() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(userRepository.findAll()).thenReturn(List.of(adminUser, normalUser));
        when(projectRepository.count()).thenReturn(5L);

        ResponseEntity<AdminUserController.AdminDashboardStatsResponse> response =
                controller.getDashboardStats("Bearer admin-token");

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().totalUsers()).isEqualTo(2);
        assertThat(response.getBody().pendingUsers()).isEqualTo(1);
        assertThat(response.getBody().activeUsers()).isEqualTo(1);
        assertThat(response.getBody().totalProjects()).isEqualTo(5);
    }

    @Test
    @DisplayName("Từ chối khi không có token")
    void requiresToken() {
        assertThatThrownBy(() -> controller.getDashboardStats(null))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    @DisplayName("Từ chối khi user không phải ADMIN")
    void nonAdmin_forbidden() {
        when(authTokenService.isTokenValid("worker-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("worker-token")).thenReturn(normalUserId);
        when(userRepository.findById(normalUserId)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> controller.getDashboardStats("Bearer worker-token"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    @DisplayName("Admin duyệt tài khoản thành công với vai trò hợp lệ (WORKER)")
    void approveUser_success() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(userRepository.findById(normalUserId)).thenReturn(Optional.of(normalUser));
        when(roleRepository.findByName("WORKER")).thenReturn(Optional.of(new Role("WORKER")));

        ResponseEntity<Map<String, Object>> response = controller.approveUser(
                normalUserId,
                new AdminUserController.ApproveRequest("WORKER"),
                "Bearer admin-token"
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(normalUser.getStatus()).isEqualTo("ACTIVE");
        assertThat(normalUser.getRole().getName()).isEqualTo("WORKER");
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("Admin từ chối tài khoản thành công")
    void rejectUser_success() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        when(userRepository.findById(normalUserId)).thenReturn(Optional.of(normalUser));

        ResponseEntity<Map<String, Object>> response = controller.rejectUser(
                normalUserId,
                new AdminUserController.RejectRequest("Không đủ hồ sơ"),
                "Bearer admin-token"
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(normalUser.getStatus()).isEqualTo("REJECTED");
        assertThat(normalUser.getRejectionReason()).isEqualTo("Không đủ hồ sơ");
        verify(userRepository).save(normalUser);
    }

    @Test
    @DisplayName("Admin đổi vai trò người dùng thành công với 6 vai trò nghiệp vụ (PROJECT_MANAGER, CUSTOMER, SITE_MANAGER, SITE_ENGINEER, WORKER, STAFF)")
    void changeRole_success() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(normalUserId)).thenReturn(Optional.of(normalUser));

        List<String> businessRoles = List.of(
                "PROJECT_MANAGER", "CUSTOMER", "SITE_MANAGER", "SITE_ENGINEER", "WORKER", "STAFF"
        );

        for (String roleName : businessRoles) {
            when(roleRepository.findByName(roleName)).thenReturn(Optional.of(new Role(roleName)));

            ResponseEntity<Map<String, Object>> response = controller.changeUserRole(
                    normalUserId,
                    new AdminUserController.RoleChangeRequest(roleName),
                    "Bearer admin-token"
            );

            assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
            assertThat(normalUser.getRole().getName()).isEqualTo(roleName);
        }
        verify(userRepository, times(businessRoles.size())).save(normalUser);
    }

    @Test
    @DisplayName("Không được phép thay đổi vai trò của tài khoản quản trị viên chính (admin@admin.com)")
    void changeRole_adminAccount_throwsBadRequest() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        assertThatThrownBy(() -> controller.changeUserRole(
                adminId,
                new AdminUserController.RoleChangeRequest("WORKER"),
                "Bearer admin-token"
        )).isInstanceOf(com.ntdhtcct.common.exception.BadRequestException.class)
          .hasMessageContaining("Không thể thay đổi vai trò của tài khoản quản trị viên chính.");
    }

    @Test
    @DisplayName("Từ chối khi cố phân vai trò legacy không thuộc 6 vai trò nghiệp vụ (VIEWER/INVALID)")
    void changeRole_invalidRole_throwsBadRequest() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(userRepository.findById(normalUserId)).thenReturn(Optional.of(normalUser));

        assertThatThrownBy(() -> controller.changeUserRole(
                normalUserId,
                new AdminUserController.RoleChangeRequest("VIEWER"),
                "Bearer admin-token"
        )).isInstanceOf(com.ntdhtcct.common.exception.BadRequestException.class);
    }

    @Test
    @DisplayName("GET /api/admin/roles trả về đúng 6 vai trò nghiệp vụ theo backlog")
    void getRoles_returnsSixBusinessRoles() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));

        ResponseEntity<List<String>> response = controller.getRoles(false, "Bearer admin-token");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsExactly(
                "PROJECT_MANAGER", "CUSTOMER", "SITE_MANAGER", "SITE_ENGINEER", "WORKER", "STAFF"
        );
    }

    @Test
    @DisplayName("GET /api/admin/roles?all=true trả về toàn bộ vai trò từ DB")
    void getRoles_all_returnsAllRoles() {
        when(authTokenService.isTokenValid("admin-token")).thenReturn(true);
        when(authTokenService.getUserIdFromToken("admin-token")).thenReturn(adminId);
        when(userRepository.findById(adminId)).thenReturn(Optional.of(adminUser));
        when(roleRepository.findAll()).thenReturn(List.of(
                new Role("ADMIN"), new Role("CUSTOMER"), new Role("PROJECT_MANAGER"),
                new Role("SITE_ENGINEER"), new Role("SITE_MANAGER"), new Role("STAFF"),
                new Role("VIEWER"), new Role("WORKER")
        ));

        ResponseEntity<List<String>> response = controller.getRoles(true, "Bearer admin-token");
        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).contains(
                "ADMIN", "CUSTOMER", "PROJECT_MANAGER", "SITE_ENGINEER", "SITE_MANAGER", "STAFF", "VIEWER", "WORKER"
        );
    }
}
