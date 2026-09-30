package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.project.Project;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class WbsController {

    private final WbsService wbsService;
    private final AuthTokenService authTokenService;

    public WbsController(
            WbsService wbsService,
            AuthTokenService authTokenService
    ) {
        this.wbsService = wbsService;
        this.authTokenService = authTokenService;
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
            @PathVariable UUID projectId,
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

    @PostMapping("/projects/{projectId}/wbs")
    public ResponseEntity<?> createWbs(
            @PathVariable UUID projectId,
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

    @PutMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> updateWbs(
            @PathVariable UUID projectId,
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
                    wbsService.update(projectId, itemId, item)
            );
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(
                    new ApiResponse(false, e.getMessage())
            );
        }
    }

    @DeleteMapping("/projects/{projectId}/wbs/{itemId}")
    public ResponseEntity<?> deleteWbs(
            @PathVariable UUID projectId,
            @PathVariable UUID itemId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
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

    private boolean isAuthorized(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return false;
        }

        String token = authorization.substring(7);
        return authTokenService.isTokenValid(token);
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(401).body(
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