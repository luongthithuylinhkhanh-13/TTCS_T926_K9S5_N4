package com.ntdhtcct.domain.project;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.auth.context.UserSecurityContext;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.domain.project.dto.SiteLocationResponse;
import com.ntdhtcct.domain.project.dto.UpdateSiteLocationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/site-location")
public class ProjectSiteLocationController {

    private final ProjectSiteLocationService projectSiteLocationService;

    public ProjectSiteLocationController(ProjectSiteLocationService projectSiteLocationService) {
        this.projectSiteLocationService = projectSiteLocationService;
    }

    @GetMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<ApiResponse<SiteLocationResponse>> getSiteLocation(
            @PathVariable("projectId") UUID projectId
    ) {
        UUID userId = UserSecurityContext.getUserId();
        SiteLocationResponse response = projectSiteLocationService.getSiteLocation(projectId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy cấu hình vị trí công trường thành công", response));
    }

    @PutMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public ResponseEntity<ApiResponse<SiteLocationResponse>> updateSiteLocation(
            @PathVariable("projectId") UUID projectId,
            @Valid @RequestBody UpdateSiteLocationRequest request
    ) {
        UUID userId = UserSecurityContext.getUserId();
        SiteLocationResponse response = projectSiteLocationService.updateSiteLocation(projectId, request, userId);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật cấu hình vị trí công trường thành công", response));
    }
}
