package com.ntdhtcct.domain.wbs;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.project.Project;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WbsController {

    private final WbsService wbsService;
    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;
    private final com.ntdhtcct.domain.project.ProjectMemberService projectMemberService;

    public WbsController(
            WbsService wbsService,
            AuthTokenService authTokenService,
            ObjectMapper objectMapper,
            com.ntdhtcct.domain.project.ProjectMemberService projectMemberService
    ) {
        this.wbsService = wbsService;
        this.authTokenService = authTokenService;
        this.objectMapper = objectMapper;
        this.projectMemberService = projectMemberService;
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjects(
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }

        List<Project> projects = wbsService.getProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{projectId}/wbs")
    public ResponseEntity<?> getWbs(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.getWbsByProject(projectId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @GetMapping("/projects/{projectId}/schedule")
    public ResponseEntity<?> getSchedule(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(wbsService.getSchedule(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @GetMapping("/projects/{projectId}/critical-path")
    public ResponseEntity<?> getCriticalPath(
            @PathVariable UUID projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(wbsService.getCriticalPathProgress(projectId));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PostMapping("/projects/{projectId}/wbs")
    public ResponseEntity<?> createWbs(
            @PathVariable UUID projectId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.create(projectId, item)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<?> createTask(
            @PathVariable UUID projectId,
            @RequestBody JsonNode request,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            JsonNode duration = request.get("duration");
            if (duration == null
                    || !duration.isIntegralNumber()
                    || !duration.canConvertToInt()
                    || duration.intValue() <= 0) {
                return ResponseEntity.badRequest().body(
                        new ApiResponse(false, "Thời lượng thực hiện phải lớn hơn 0")
                );
            }

            WbsItem item = objectMapper.convertValue(request, WbsItem.class);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(wbsService.createTask(projectId, item));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PutMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> updateWbs(
            @PathVariable UUID projectId,
            @PathVariable UUID itemId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.update(projectId, itemId, item)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable UUID projectId,
            @PathVariable UUID taskId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        return updateWbs(projectId, taskId, item, authorization);
    }

    @DeleteMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> deleteWbs(
            @PathVariable UUID projectId,
            @PathVariable UUID itemId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized(null);
        }
        if (!checkAccess(projectId, authorization)) {
            return forbidden();
        }

        try {
            wbsService.delete(projectId, itemId);

            return ResponseEntity.ok(
                    new ApiResponse(true, "Xóa công việc thành công")
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    private boolean checkAccess(UUID projectId, String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String token = authorization.substring(7);
        if (!authTokenService.isTokenValid(token)) {
            return false;
        }
        UUID userId = authTokenService.getUserIdFromToken(token);
        if (userId == null) {
            return false;
        }
        return projectMemberService.hasAccess(projectId, userId);
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        String token = authorization.substring(7);
        return authTokenService.isTokenValid(token);
    }

    private ResponseEntity<ApiResponse> unauthorized(String message) {
        return ResponseEntity.status(401).body(
                new ApiResponse(
                        false,
                        message != null ? message : "Token không hợp lệ hoặc đã hết hạn"
                )
        );
    }

    private ResponseEntity<ApiResponse> forbidden() {
        return ResponseEntity.status(403).body(
                new ApiResponse(
                        false,
                        "Bạn không có quyền truy cập vào dự án này"
                )
        );
    }

    public record ApiResponse(
            boolean success,
            String message
    ) {
    }
}