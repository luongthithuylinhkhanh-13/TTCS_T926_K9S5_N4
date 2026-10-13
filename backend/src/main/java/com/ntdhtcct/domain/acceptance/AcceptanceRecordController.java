package com.ntdhtcct.domain.acceptance;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ntdhtcct.auth.annotation.RequireProjectRole;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordAuditLogResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordRequest;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordResponse;
import com.ntdhtcct.domain.acceptance.dto.AcceptanceRecordReturnRequest;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/projects/{projectId}/acceptance-records")
public class AcceptanceRecordController {

    private final AcceptanceRecordService acceptanceRecordService;

    public AcceptanceRecordController(AcceptanceRecordService acceptanceRecordService) {
        this.acceptanceRecordService = acceptanceRecordService;
    }

    @GetMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public List<AcceptanceRecordResponse> getRecords(@PathVariable UUID projectId) {
        return acceptanceRecordService.getRecords(projectId).stream()
                .map(AcceptanceRecordResponse::from)
                .toList();
    }

    @PostMapping
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public ResponseEntity<AcceptanceRecordResponse> create(
            @PathVariable UUID projectId,
            @Valid @RequestBody AcceptanceRecordRequest request
    ) {
        AcceptanceRecord created = acceptanceRecordService.create(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(AcceptanceRecordResponse.from(created));
    }

    @GetMapping("/{recordId}")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public AcceptanceRecordResponse getRecord(
            @PathVariable UUID projectId,
            @PathVariable UUID recordId
    ) {
        return AcceptanceRecordResponse.from(acceptanceRecordService.getRecord(projectId, recordId));
    }

    @PutMapping("/{recordId}")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER"})
    public AcceptanceRecordResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID recordId,
            @Valid @RequestBody AcceptanceRecordRequest request
    ) {
        return AcceptanceRecordResponse.from(
                acceptanceRecordService.update(projectId, recordId, request)
        );
    }

    @GetMapping("/{recordId}/history")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER", "SITE_ENGINEER", "WORKER", "VIEWER"})
    public List<AcceptanceRecordAuditLogResponse> getHistory(
            @PathVariable UUID projectId,
            @PathVariable UUID recordId
    ) {
        return acceptanceRecordService.getHistory(projectId, recordId).stream()
                .map(AcceptanceRecordAuditLogResponse::from)
                .toList();
    }

    @PostMapping("/{recordId}/approve")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public AcceptanceRecordResponse approve(
            @PathVariable UUID projectId,
            @PathVariable UUID recordId
    ) {
        return AcceptanceRecordResponse.from(
                acceptanceRecordService.approve(projectId, recordId)
        );
    }

    @PostMapping("/{recordId}/return")
    @RequireProjectRole({"ADMIN", "PROJECT_MANAGER"})
    public AcceptanceRecordResponse returnForChanges(
            @PathVariable UUID projectId,
            @PathVariable UUID recordId,
            @Valid @RequestBody AcceptanceRecordReturnRequest request
    ) {
        String comment = request == null ? null : request.comment();
        return AcceptanceRecordResponse.from(
                acceptanceRecordService.returnForChanges(projectId, recordId, comment)
        );
    }
}
