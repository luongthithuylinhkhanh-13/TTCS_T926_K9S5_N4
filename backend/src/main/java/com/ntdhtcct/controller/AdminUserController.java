package com.ntdhtcct.controller;

import com.ntdhtcct.common.exception.BadRequestException;
import com.ntdhtcct.common.exception.ForbiddenException;
import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.common.exception.UnauthorizedException;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.project.Project;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.Role;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.RoleRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {

    public static final List<String> BUSINESS_ROLES_LIST = List.of(
            "PROJECT_MANAGER",
            "CUSTOMER",
            "SITE_MANAGER",
            "SITE_ENGINEER",
            "WORKER",
            "STAFF"
    );
    public static final Set<String> ALLOWED_ROLES = Set.of(
            "ADMIN",
            "PROJECT_MANAGER",
            "CUSTOMER",
            "SITE_MANAGER",
            "SITE_ENGINEER",
            "WORKER",
            "STAFF"
    );
    public static final List<String> ALLOWED_ROLES_LIST = BUSINESS_ROLES_LIST;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuthTokenService authTokenService;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    public AdminUserController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthTokenService authTokenService,
            ProjectRepository projectRepository
    ) {
        this(userRepository, roleRepository, authTokenService, projectRepository, null);
    }

    @Autowired
    public AdminUserController(
            UserRepository userRepository,
            RoleRepository roleRepository,
            AuthTokenService authTokenService,
            ProjectRepository projectRepository,
            ProjectMemberRepository projectMemberRepository
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.authTokenService = authTokenService;
        this.projectRepository = projectRepository;
        this.projectMemberRepository = projectMemberRepository;
    }

    private User requireAdmin(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new UnauthorizedException("AUTH_MISSING_TOKEN", "Yêu cầu đăng nhập để truy cập chức năng này.");
        }
        String token = authorization.substring(7).trim();
        if (!authTokenService.isTokenValid(token)) {
            throw new UnauthorizedException("AUTH_INVALID_TOKEN", "Phiên đăng nhập không hợp lệ hoặc đã hết hạn.");
        }
        UUID userId = authTokenService.getUserIdFromToken(token);
        if (userId == null) {
            throw new UnauthorizedException("AUTH_INVALID_TOKEN", "Không xác định được người dùng.");
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || user.getRole() == null || !"ADMIN".equalsIgnoreCase(user.getRole().getName())) {
            throw new ForbiddenException("ADMIN_REQUIRED", "Bạn không có quyền truy cập chức năng này. Yêu cầu quyền ADMIN.");
        }
        return user;
    }

    /**
     * GET /api/admin/dashboard-stats
     * Tổng quan thống kê hệ thống cho Admin Dashboard
     */
    @GetMapping("/dashboard-stats")
    public ResponseEntity<AdminDashboardStatsResponse> getDashboardStats(
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        List<User> allUsers = userRepository.findAll();
        long totalUsers = allUsers.size();
        long pendingUsers = allUsers.stream().filter(u -> "PENDING".equalsIgnoreCase(u.getStatus())).count();
        long activeUsers = allUsers.stream().filter(u -> "ACTIVE".equalsIgnoreCase(u.getStatus())).count();
        long rejectedUsers = allUsers.stream().filter(u -> "REJECTED".equalsIgnoreCase(u.getStatus()) || "DISABLED".equalsIgnoreCase(u.getStatus())).count();
        long totalProjects = projectRepository.count();

        List<UserSummaryResponse> recentPending = allUsers.stream()
                .filter(u -> "PENDING".equalsIgnoreCase(u.getStatus()))
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(5)
                .map(this::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(new AdminDashboardStatsResponse(
                totalUsers,
                pendingUsers,
                activeUsers,
                rejectedUsers,
                totalProjects,
                "HEALTHY",
                recentPending
        ));
    }

    /**
     * GET /api/admin/users
     * Lấy danh sách toàn bộ tài khoản
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserSummaryResponse>> getUsers(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        List<UserSummaryResponse> list = userRepository.findAll().stream()
                .filter(u -> {
                    if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
                        if (!status.equalsIgnoreCase(u.getStatus())) return false;
                    }
                    if (role != null && !role.isBlank() && !"ALL".equalsIgnoreCase(role)) {
                        String userRole = u.getRole() != null ? u.getRole().getName() : "";
                        if (!role.equalsIgnoreCase(userRole)) return false;
                    }
                    if (search != null && !search.isBlank()) {
                        String keyword = search.trim().toLowerCase();
                        boolean matchEmail = u.getEmail() != null && u.getEmail().toLowerCase().contains(keyword);
                        boolean matchName = u.getFullName() != null && u.getFullName().toLowerCase().contains(keyword);
                        if (!matchEmail && !matchName) return false;
                    }
                    return true;
                })
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toSummaryResponse)
                .toList();

        return ResponseEntity.ok(list);
    }

    /**
     * PUT /api/admin/users/{userId}/approve
     * Duyệt tài khoản thành viên (PENDING -> ACTIVE)
     */
    @PutMapping("/users/{userId}/approve")
    public ResponseEntity<Map<String, Object>> approveUser(
            @PathVariable UUID userId,
            @RequestBody(required = false) ApproveRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        String targetRoleName = "WORKER";
        if (request != null && request.roleName() != null && !request.roleName().isBlank()) {
            targetRoleName = request.roleName().trim().toUpperCase();
        } else if (user.getRole() != null && ALLOWED_ROLES.contains(user.getRole().getName().toUpperCase())) {
            targetRoleName = user.getRole().getName().toUpperCase();
        }

        if (!ALLOWED_ROLES.contains(targetRoleName)) {
            throw new BadRequestException("INVALID_ROLE",
                    "Vai trò không hợp lệ. Chỉ được phân một trong 6 vai trò nghiệp vụ hoặc ADMIN.");
        }

        final String finalRoleName = targetRoleName;
        Role role = roleRepository.findByName(finalRoleName)
                .orElseGet(() -> roleRepository.save(new Role(finalRoleName)));

        user.setRole(role);
        user.setStatus("ACTIVE");
        user.setRejectionReason(null);
        user.setEmailVerified(true);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Phê duyệt tài khoản thành công",
                "userId", user.getId(),
                "status", "ACTIVE",
                "roleName", role.getName()
        ));
    }

    /**
     * PUT /api/admin/users/{userId}/reject
     * Từ chối tài khoản thành viên (PENDING -> REJECTED)
     */
    @PutMapping("/users/{userId}/reject")
    public ResponseEntity<Map<String, Object>> rejectUser(
            @PathVariable UUID userId,
            @RequestBody(required = false) RejectRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        String reason = (request != null && request.reason() != null && !request.reason().isBlank())
                ? request.reason().trim()
                : "Không đủ điều kiện tham gia hệ thống";

        user.setStatus("REJECTED");
        user.setRejectionReason(reason);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Đã từ chối tài khoản",
                "userId", user.getId(),
                "status", "REJECTED",
                "rejectionReason", reason
        ));
    }

    /**
     * PUT /api/admin/users/{userId}/role
     * Thay đổi vai trò (Phân quyền) cho người dùng
     */
    @PutMapping("/users/{userId}/role")
    public ResponseEntity<Map<String, Object>> changeUserRole(
            @PathVariable UUID userId,
            @RequestBody RoleChangeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        if (request == null || request.roleName() == null || request.roleName().isBlank()) {
            throw new BadRequestException("INVALID_ROLE", "Vui lòng chỉ định vai trò hợp lệ.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        if ("admin@admin.com".equalsIgnoreCase(user.getEmail())) {
            throw new BadRequestException("ADMIN_IMMUTABLE",
                    "Không thể thay đổi vai trò của tài khoản quản trị viên chính.");
        }

        String targetRole = request.roleName().trim().toUpperCase();
        if (!ALLOWED_ROLES.contains(targetRole)) {
            throw new BadRequestException("INVALID_ROLE",
                    "Vai trò không hợp lệ. Chỉ được phân một trong 6 vai trò nghiệp vụ hoặc ADMIN.");
        }

        Role role = roleRepository.findByName(targetRole)
                .orElseGet(() -> roleRepository.save(new Role(targetRole)));

        user.setRole(role);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật vai trò thành công",
                "userId", user.getId(),
                "roleName", role.getName()
        ));
    }

    /**
     * PUT /api/admin/users/{userId}/status
     * Khóa hoặc mở khóa tài khoản
     */
    @PutMapping("/users/{userId}/status")
    public ResponseEntity<Map<String, Object>> changeUserStatus(
            @PathVariable UUID userId,
            @RequestBody StatusChangeRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        if (request == null || request.status() == null || request.status().isBlank()) {
            throw new BadRequestException("INVALID_STATUS", "Vui lòng chỉ định trạng thái hợp lệ.");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng"));

        String newStatus = request.status().trim().toUpperCase();
        user.setStatus(newStatus);
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Cập nhật trạng thái tài khoản thành công",
                "userId", user.getId(),
                "status", newStatus
        ));
    }

    /**
     * GET /api/admin/roles
     * Danh sách 6 vai trò nghiệp vụ theo backlog (hoặc toàn bộ nếu all=true)
     */
    @GetMapping("/roles")
    public ResponseEntity<List<String>> getRoles(
            @RequestParam(value = "all", defaultValue = "false") boolean all,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);

        if (all) {
            List<String> roles = roleRepository.findAll().stream()
                    .map(Role::getName)
                    .distinct()
                    .sorted()
                    .toList();
            return ResponseEntity.ok(roles);
        }

        return ResponseEntity.ok(ALLOWED_ROLES_LIST);
    }

    /**
     * GET /api/admin/projects/{projectId}/manager
     * Lấy danh sách Ban quản lý hiện tại của dự án
     */
    @GetMapping("/projects/{projectId}/manager")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProjectMemberResponse>> getProjectManager(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        if (projectMemberRepository == null) {
            return ResponseEntity.ok(List.of());
        }
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException("PROJECT_NOT_FOUND", "Không tìm thấy dự án với ID: " + projectId);
        }

        List<ProjectMemberResponse> managers = projectMemberRepository.findByProjectId(projectId).stream()
                .filter(pm -> "ACTIVE".equalsIgnoreCase(pm.getStatus())
                        && pm.getRole() != null
                        && "PROJECT_MANAGER".equalsIgnoreCase(pm.getRole().getName()))
                .map(ProjectMemberResponse::fromEntity)
                .toList();

        return ResponseEntity.ok(managers);
    }

    /**
     * POST /api/admin/projects/{projectId}/manager
     * Admin phân Ban quản lý dự án cho một công trình
     */
    @PostMapping("/projects/{projectId}/manager")
    @Transactional
    public ResponseEntity<ProjectMemberResponse> assignProjectManager(
            @PathVariable UUID projectId,
            @RequestBody AssignProjectManagerRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        requireAdmin(authorization);
        if (projectMemberRepository == null) {
            throw new BadRequestException("SERVICE_UNAVAILABLE", "Hệ thống quản lý thành viên chưa sẵn sàng.");
        }

        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new ResourceNotFoundException("PROJECT_NOT_FOUND", "Không tìm thấy dự án với ID: " + projectId));

        if (request == null || request.userId() == null) {
            throw new BadRequestException("USER_ID_REQUIRED", "Yêu cầu chọn tài khoản người dùng để phân quyền.");
        }

        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "Không tìm thấy người dùng."));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BadRequestException("USER_NOT_ACTIVE", "Chỉ có thể phân quyền cho tài khoản đã được kích hoạt (ACTIVE).");
        }

        if ("admin@admin.com".equalsIgnoreCase(user.getEmail())
                || (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().getName()))) {
            throw new BadRequestException("CANNOT_ASSIGN_ADMIN", "Không thể phân tài khoản Quản trị hệ thống làm Ban quản lý dự án.");
        }

        Role projectManagerRole = roleRepository.findByName("PROJECT_MANAGER")
                .orElseThrow(() -> new ResourceNotFoundException("ROLE_NOT_FOUND", "Vai trò PROJECT_MANAGER không tồn tại trong hệ thống."));

        Optional<ProjectMember> existingOpt = projectMemberRepository.findByProjectIdAndUserId(projectId, user.getId());
        ProjectMember memberToSave;
        if (existingOpt.isPresent()) {
            memberToSave = existingOpt.get();
            memberToSave.setRole(projectManagerRole);
            memberToSave.setStatus("ACTIVE");
            memberToSave.setJoinedAt(OffsetDateTime.now());
        } else {
            memberToSave = new ProjectMember(project, user, projectManagerRole);
            memberToSave.setStatus("ACTIVE");
            memberToSave.setJoinedAt(OffsetDateTime.now());
        }

        ProjectMember saved = projectMemberRepository.save(memberToSave);
        return ResponseEntity.ok(ProjectMemberResponse.fromEntity(saved));
    }

    private UserSummaryResponse toSummaryResponse(User u) {
        return new UserSummaryResponse(
                u.getId(),
                u.getFullName(),
                u.getEmail(),
                u.getPhoneNumber(),
                u.getRole() != null ? u.getRole().getName() : "CUSTOMER",
                u.getStatus() != null ? u.getStatus() : "PENDING",
                u.getRejectionReason(),
                u.isEmailVerified(),
                u.getCreatedAt(),
                u.getUpdatedAt()
        );
    }

    public record UserSummaryResponse(
            UUID id,
            String fullName,
            String email,
            String phoneNumber,
            String roleName,
            String status,
            String rejectionReason,
            boolean emailVerified,
            OffsetDateTime createdAt,
            OffsetDateTime updatedAt
    ) {}

    public record AdminDashboardStatsResponse(
            long totalUsers,
            long pendingUsers,
            long activeUsers,
            long rejectedUsers,
            long totalProjects,
            String systemStatus,
            List<UserSummaryResponse> recentPendingUsers
    ) {}

    public record ApproveRequest(String roleName) {}
    public record RejectRequest(String reason) {}
    public record RoleChangeRequest(String roleName) {}
    public record StatusChangeRequest(String status) {}
    public record AssignProjectManagerRequest(UUID userId) {}
}
