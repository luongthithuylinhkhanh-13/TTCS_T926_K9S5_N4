package com.ntdhtcct.controller;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.common.response.ApiResponse;
import com.ntdhtcct.dto.*;
import com.ntdhtcct.service.ScheduleService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller xử lý các nghiệp vụ thuộc Epic E-04 (Tiến độ và đường găng),
 * Story S-08 (Tính thời điểm khởi sớm và kết thúc sớm - NTDHTCT-143):
 * - T-18 (NTDHTCT-144): Xây dựng quan hệ phụ thuộc (FS, SS, FF, SF).
 * - T-19 (NTDHTCT-145): Tính ES và EF (Forward Pass).
 */
@RestController
@RequestMapping("/api/projects/{projectId}")
public class ScheduleController {

    private final ScheduleService scheduleService;

    public ScheduleController(ScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    /**
     * Tạo công việc mới trong dự án.
     */
    @PostMapping("/tasks")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<TaskResponse>> createTask(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTaskRequest request) {

        TaskResponse response = scheduleService.createTask(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo công việc thành công", response));
    }

    /**
     * Lấy danh sách công việc của dự án.
     */
    @GetMapping("/tasks")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<ApiResponse<List<TaskResponse>>> getTasks(@PathVariable Long projectId) {
        List<TaskResponse> tasks = scheduleService.getTasks(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách công việc thành công", tasks));
    }

    /**
     * T-18 (NTDHTCT-144): Tạo quan hệ phụ thuộc giữa 2 công việc (FS, SS, FF, SF).
     */
    @PostMapping("/dependencies")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<TaskDependencyResponse>> createDependency(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateDependencyRequest request) {

        TaskDependencyResponse response = scheduleService.createDependency(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Tạo quan hệ phụ thuộc công việc thành công", response));
    }

    /**
     * T-18: Lấy danh sách các quan hệ phụ thuộc trong dự án.
     */
    @GetMapping("/dependencies")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<ApiResponse<List<TaskDependencyResponse>>> getDependencies(@PathVariable Long projectId) {
        List<TaskDependencyResponse> deps = scheduleService.getDependencies(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy danh sách quan hệ phụ thuộc thành công", deps));
    }

    /**
     * T-19 (NTDHTCT-145): Kích hoạt thuật toán Forward Pass tính toán Khởi sớm (ES) và Kết thúc sớm (EF).
     * Mặc định lưu kết quả cập nhật vào Database (saveToDb = true).
     */
    @PostMapping("/schedule/forward-pass")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<ApiResponse<ScheduleCalculationResponse>> calculateForwardPass(
            @PathVariable Long projectId,
            @RequestParam(defaultValue = "true") boolean saveToDb) {

        ScheduleCalculationResponse response = scheduleService.calculateForwardPass(projectId, saveToDb);
        return ResponseEntity.ok(ApiResponse.ok("Tính toán thời điểm khởi sớm (ES) và kết thúc sớm (EF) thành công", response));
    }

    /**
     * T-19: Xem kết quả tiến độ (ES/EF) đã tính toán của dự án.
     */
    @GetMapping("/schedule/forward-pass")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public ResponseEntity<ApiResponse<ScheduleCalculationResponse>> getSchedule(@PathVariable Long projectId) {
        ScheduleCalculationResponse response = scheduleService.getSchedule(projectId);
        return ResponseEntity.ok(ApiResponse.ok("Lấy kết quả tiến độ thành công", response));
    }
}
