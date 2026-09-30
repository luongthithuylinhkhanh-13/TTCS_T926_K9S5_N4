package com.ntdhtcct.controller;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.common.response.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller minh họa kiểm thử cơ chế phân quyền chi tiết (RBAC) và Default Deny:
 * - T-04.7: Kiểm tra Role của User
 * - T-04.8: Cấu hình quyền theo từng Route
 * - T-04.9: Thiết lập cơ chế Default Deny
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class DemoProjectProtectedController {

    /**
     * Tuyến đường chỉ dành cho SITE_ENGINEER hoặc PROJECT_MANAGER.
     * Người có vai trò WORKER hoặc VIEWER sẽ bị chặn 403 Forbidden.
     */
    @GetMapping("/engineering-diary")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<Map<String, String>>> getEngineeringDiary(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Truy cập thành công nhật ký công trường",
                Map.of("projectId", String.valueOf(projectId), "feature", "Nhật ký công trường")));
    }

    /**
     * T-04.9: Tuyến đường KHÔNG được gắn Annotation @RequireProjectRole
     * và KHÔNG có trong RoutePermissionConfig.
     * Cơ chế Default Deny sẽ tự động CHẶN TOÀN BỘ và trả về 403 Forbidden!
     */
    @GetMapping("/unconfigured-secure-endpoint")
    public ResponseEntity<ApiResponse<String>> unconfiguredEndpoint(@PathVariable Long projectId) {
        return ResponseEntity.ok(ApiResponse.ok("Nếu bạn thấy chuỗi này, Default Deny đã thất bại!"));
    }
}
