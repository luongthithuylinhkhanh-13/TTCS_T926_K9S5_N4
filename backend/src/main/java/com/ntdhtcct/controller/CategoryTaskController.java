package com.ntdhtcct.controller;

import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.wbs.CategoryTaskService;
import com.ntdhtcct.domain.wbs.Task;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/projects/{projectId}/categories/{categoryId}/tasks")
public class CategoryTaskController {

    private final CategoryTaskService categoryTaskService;
    private final AuthTokenService authTokenService;

    public CategoryTaskController(
            CategoryTaskService categoryTaskService,
            AuthTokenService authTokenService
    ) {
        this.categoryTaskService = categoryTaskService;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public ResponseEntity<?> getTasks(
            @PathVariable UUID projectId,
            @PathVariable UUID categoryId,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            return ResponseEntity.ok(categoryTaskService.getTasks(projectId, categoryId)
                    .stream()
                    .map(TaskResponse::from)
                    .toList());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createTask(
            @PathVariable UUID projectId,
            @PathVariable UUID categoryId,
            @Valid @RequestBody CreateTaskRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            Task task = categoryTaskService.createTask(
                    projectId,
                    categoryId,
                    request.name(),
                    request.duration()
            );
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(TaskResponse.from(task));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ApiResponse(false, e.getMessage()));
        }
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return false;
        }
        return authTokenService.isTokenValid(authorization.substring(7));
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(false, "Token không hợp lệ hoặc đã hết hạn"));
    }

    public record CreateTaskRequest(
            @NotBlank(message = "Tên công việc không được để trống")
            @Size(max = 255, message = "Tên công việc không được vượt quá 255 ký tự")
            String name,

            @NotNull(message = "Thời lượng không được để trống")
            @Positive(message = "Thời lượng phải lớn hơn 0")
            Integer duration
    ) {
    }

    public record TaskResponse(
            UUID id,
            UUID categoryId,
            String name,
            Integer duration,
            OffsetDateTime createdAt
    ) {
        private static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getCategory().getId(),
                    task.getName(),
                    task.getDuration(),
                    task.getCreatedAt()
            );
        }
    }

    public record ApiResponse(boolean success, String message) {
    }
}