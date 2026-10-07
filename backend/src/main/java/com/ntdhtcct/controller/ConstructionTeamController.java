package com.ntdhtcct.controller;

import java.time.LocalDate;
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

import com.ntdhtcct.domain.auth.AuthTokenService;
import com.ntdhtcct.domain.wbs.ConstructionTeam;
import com.ntdhtcct.domain.wbs.ConstructionTeamScheduleService;
import com.ntdhtcct.domain.wbs.ConstructionTeamService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/projects/{projectId}/construction-teams")
public class ConstructionTeamController {

    private final ConstructionTeamService constructionTeamService;
    private final ConstructionTeamScheduleService constructionTeamScheduleService;
    private final AuthTokenService authTokenService;

    public ConstructionTeamController(
            ConstructionTeamService constructionTeamService,
            ConstructionTeamScheduleService constructionTeamScheduleService,
            AuthTokenService authTokenService
    ) {
        this.constructionTeamService = constructionTeamService;
        this.constructionTeamScheduleService = constructionTeamScheduleService;
        this.authTokenService = authTokenService;
    }

    @GetMapping
    public ResponseEntity<?> getTeams(
            @PathVariable Long projectId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<ConstructionTeam> teams =
                    constructionTeamService.getTeams(projectId);

            return ResponseEntity.ok(
                    teams.stream()
                            .map(TeamResponse::from)
                            .toList()
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> createTeam(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateTeamRequest request,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            ConstructionTeam team =
                    constructionTeamService.createTeam(
                            projectId,
                            request.code(),
                            request.name(),
                            request.description()
                    );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(TeamResponse.from(team));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /*
     * T-48: Gán đội thi công cho WBS
     */
    @PutMapping("/wbs/{wbsId}")
    public ResponseEntity<?> assignTeam(
            @PathVariable Long projectId,
            @PathVariable UUID wbsId,
            @RequestBody AssignTeamRequest request,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            constructionTeamService.assignTeamToWbs(
                    projectId,
                    wbsId,
                    request.teamId()
            );

            return ResponseEntity.ok(
                    new ApiResponse(
                            true,
                            "Gán đội thi công thành công"
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /*
     * T-49:
     * Kiểm tra các công việc bị chồng lịch
     * trong cùng một đội thi công.
     */
    @GetMapping("/{teamId}/schedule/conflicts")
    public ResponseEntity<?> checkScheduleConflicts(
            @PathVariable Long projectId,
            @PathVariable UUID teamId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<ConstructionTeamScheduleService.ScheduleConflict> conflicts =
                    constructionTeamScheduleService.checkTeamSchedule(
                            projectId,
                            teamId
                    );

            return ResponseEntity.ok(
                    new ScheduleConflictResponse(
                            conflicts.isEmpty(),
                            conflicts.size(),
                            conflicts
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /*
     * T-49:
     * Kiểm tra một khoảng thời gian mới
     * có bị chồng với công việc hiện tại của đội hay không.
     */
    @PostMapping("/{teamId}/schedule/check")
    public ResponseEntity<?> checkNewSchedule(
            @PathVariable Long projectId,
            @PathVariable UUID teamId,
            @RequestBody ScheduleCheckRequest request,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<ConstructionTeamScheduleService.ScheduleConflict> conflicts =
                    constructionTeamScheduleService.checkNewSchedule(
                            projectId,
                            teamId,
                            request.startDate(),
                            request.endDate(),
                            request.excludeWbsId()
                    );

            return ResponseEntity.ok(
                    new ScheduleConflictResponse(
                            conflicts.isEmpty(),
                            conflicts.size(),
                            conflicts
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /*
     * Bỏ đội thi công khỏi WBS
     */
    @DeleteMapping("/wbs/{wbsId}")
    public ResponseEntity<?> removeTeam(
            @PathVariable Long projectId,
            @PathVariable UUID wbsId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            constructionTeamService.removeTeamFromWbs(
                    projectId,
                    wbsId
            );

            return ResponseEntity.ok(
                    new ApiResponse(
                            true,
                            "Đã bỏ đội thi công khỏi công việc"
                    )
            );

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    /*
     * T-50:
     * Xem lịch sử thay đổi đội thi công của một WBS.
     */
    @GetMapping("/wbs/{wbsId}/assignment-history")
    public ResponseEntity<?> getAssignmentHistory(
            @PathVariable Long projectId,
            @PathVariable UUID wbsId,
            @RequestHeader(value = "Authorization", required = false)
            String authorization
    ) {
        if (!isAuthorized(authorization)) {
            return unauthorized();
        }

        try {
            List<com.ntdhtcct.domain.wbs.TeamAssignmentHistory> history =
                    constructionTeamService.getAssignmentHistory(
                            projectId,
                            wbsId
                    );

            return ResponseEntity.ok(history);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ApiResponse(false, e.getMessage()));
        }
    }

    private boolean isAuthorized(String authorization) {
        if (authorization == null
                || !authorization.startsWith("Bearer ")) {
            return false;
        }

        return authTokenService.isTokenValid(
                authorization.substring(7)
        );
    }

    private ResponseEntity<ApiResponse> unauthorized() {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ApiResponse(
                        false,
                        "Token không hợp lệ hoặc đã hết hạn"
                ));
    }

    public record CreateTeamRequest(
            @NotBlank(message = "Mã đội thi công không được để trống")
            @Size(
                    max = 50,
                    message = "Mã đội thi công không được vượt quá 50 ký tự"
            )
            String code,

            @NotBlank(message = "Tên đội thi công không được để trống")
            @Size(
                    max = 255,
                    message = "Tên đội thi công không được vượt quá 255 ký tự"
            )
            String name,

            String description
    ) {
    }

    public record AssignTeamRequest(
            UUID teamId
    ) {
    }

    /*
     * T-49: Dữ liệu kiểm tra lịch
     */
    public record ScheduleCheckRequest(
            LocalDate startDate,
            LocalDate endDate,
            UUID excludeWbsId
    ) {
    }

    /*
     * T-49: Kết quả kiểm tra chồng lịch
     */
    public record ScheduleConflictResponse(
            boolean available,
            int conflictCount,
            List<ConstructionTeamScheduleService.ScheduleConflict> conflicts
    ) {
    }

    public record TeamResponse(
            UUID id,
            Long projectId,
            String code,
            String name,
            String description,
            String status
    ) {
        public static TeamResponse from(ConstructionTeam team) {
            return new TeamResponse(
                    team.getId(),
                    team.getProjectId(),
                    team.getCode(),
                    team.getName(),
                    team.getDescription(),
                    team.getStatus()
            );
        }
    }

    public record ApiResponse(
            boolean success,
            String message
    ) {
    }
}