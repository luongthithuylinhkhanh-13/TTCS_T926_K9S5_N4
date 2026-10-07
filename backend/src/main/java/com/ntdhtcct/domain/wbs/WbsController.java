package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.project.Project;

@RestController
@RequestMapping("/api")
public class WbsController {

    private final WbsService wbsService;
    private final AuthTokenService authTokenService;
    private final ObjectMapper objectMapper;

    public WbsController(
            WbsService wbsService,
            AuthTokenService authTokenService,
            ObjectMapper objectMapper
    ) {
        this.wbsService = wbsService;
        this.authTokenService = authTokenService;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/projects")
    public ResponseEntity<?> getProjects(
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        List<Project> projects = wbsService.getProjects();
        return ResponseEntity.ok(projects);
    }

    @GetMapping("/projects/{projectId}/wbs")
    public ResponseEntity<?> getWbs(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
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
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.getSchedule(projectId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @GetMapping("/projects/{projectId}/critical-path")
    public ResponseEntity<?> getCriticalPath(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.getCriticalPathProgress(projectId)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PostMapping("/projects/{projectId}/wbs")
    public ResponseEntity<?> createWbs(
            @PathVariable Long projectId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
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
            @PathVariable Long projectId,
            @RequestBody JsonNode request,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            JsonNode duration = request.get("duration");

            if (duration == null
                    || !duration.isIntegralNumber()
                    || !duration.canConvertToInt()
                    || duration.intValue() <= 0) {

                return ResponseEntity.badRequest().body(
                        new ApiResponse(
                                false,
                                "Thời lượng thực hiện phải lớn hơn 0"
                        )
                );
            }

            WbsItem item =
                    objectMapper.convertValue(
                            request,
                            WbsItem.class
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(wbsService.createTask(projectId, item));

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PutMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> updateWbs(
            @PathVariable Long projectId,
            @PathVariable UUID itemId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(
                    wbsService.update(
                            projectId,
                            itemId,
                            item
                    )
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @PutMapping("/projects/{projectId}/tasks/{taskId}")
    public ResponseEntity<?> updateTask(
            @PathVariable Long projectId,
            @PathVariable UUID taskId,
            @RequestBody WbsItem item,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        return updateWbs(
                projectId,
                taskId,
                item,
                authorization
        );
    }

    @DeleteMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> deleteWbs(
            @PathVariable Long projectId,
            @PathVariable UUID itemId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            wbsService.delete(
                    projectId,
                    itemId
            );

            return ResponseEntity.ok(
                    new ApiResponse(
                            true,
                            "Xóa công việc thành công"
                    )
            );

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7);

        return authTokenService.isTokenValid(token);
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(
                        new ApiResponse(
                                false,
                                "Token không hợp lệ hoặc đã hết hạn"
                        )
                );
    }

    public record ApiResponse(
            boolean success,
            String message
    ) {
    }
}