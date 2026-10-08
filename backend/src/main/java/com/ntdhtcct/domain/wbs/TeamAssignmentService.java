package com.ntdhtcct.domain.wbs;

import com.ntdhtcct.common.exception.ResourceNotFoundException;
import com.ntdhtcct.domain.project.ProjectRepository;
import com.ntdhtcct.dto.ProjectMemberResponse;
import com.ntdhtcct.entity.ProjectMember;
import com.ntdhtcct.entity.User;
import com.ntdhtcct.repository.ProjectMemberRepository;
import com.ntdhtcct.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TeamAssignmentService {

    private final ProjectRepository projectRepository;
    private final WbsItemRepository wbsItemRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserRepository userRepository;
    private final TeamAssignmentHistoryRepository historyRepository;

    public TeamAssignmentService(
            ProjectRepository projectRepository,
            WbsItemRepository wbsItemRepository,
            ProjectMemberRepository projectMemberRepository,
            UserRepository userRepository,
            TeamAssignmentHistoryRepository historyRepository
    ) {
        this.projectRepository = projectRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.projectMemberRepository = projectMemberRepository;
        this.userRepository = userRepository;
        this.historyRepository = historyRepository;
    }

    @Transactional(readOnly = true)
    public List<ProjectMemberResponse> getProjectCrews(UUID projectId) {
        requireProject(projectId);

        return projectMemberRepository.findByProjectId(projectId).stream()
                .filter(member -> "ACTIVE".equalsIgnoreCase(member.getStatus()))
                .filter(member -> member.getRole() != null
                        && "WORKER".equalsIgnoreCase(member.getRole().getName()))
                .map(ProjectMemberResponse::fromEntity)
                .toList();
    }

    @Transactional
    public AssignmentResult assignCrew(
            UUID projectId,
            UUID taskId,
            UUID teamMemberId,
            UUID actorId
    ) {
        requireProject(projectId);
        WbsItem task = requireTask(projectId, taskId);
        User actor = userRepository.findById(actorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "USER_NOT_FOUND",
                        "Không tìm thấy tài khoản thực hiện phân công."
                ));

        ProjectMember crew = teamMemberId == null
                ? null
                : requireProjectCrew(projectId, teamMemberId);

        UUID nextId = crew == null ? null : crew.getUser().getId();
        String nextName = crew == null
                ? null
                : crew.getUser().getFullName() == null
                    || crew.getUser().getFullName().isBlank()
                    ? crew.getUser().getEmail()
                    : crew.getUser().getFullName();

        List<TeamAssignmentConflictDetector.Overlap> overlaps =
                TeamAssignmentConflictDetector.findOverlaps(
                        task,
                        nextId,
                        wbsItemRepository.findByProjectIdOrderByWbsCodeAsc(projectId)
                );

        UUID previousId = task.getAssignedTeamMemberId();
        String previousName = task.getAssignedTeamName();
        boolean changed = !java.util.Objects.equals(previousId, nextId);

        if (changed) {
            task.setAssignedTeamMemberId(nextId);
            task.setAssignedTeamName(nextName);
            wbsItemRepository.save(task);

            historyRepository.save(new TeamAssignmentHistory(
                    projectId,
                    taskId,
                    previousId,
                    previousName,
                    nextId,
                    nextName,
                    actorId,
                    actor.getFullName() == null || actor.getFullName().isBlank()
                            ? actor.getEmail()
                            : actor.getFullName()
            ));
        }

        return new AssignmentResult(task, overlaps, changed);
    }

    @Transactional(readOnly = true)
    public List<TeamAssignmentHistory> getAssignmentHistory(
            UUID projectId,
            UUID taskId
    ) {
        requireProject(projectId);
        requireTask(projectId, taskId);
        return historyRepository
                .findByProjectIdAndWbsItemIdOrderByChangedAtDesc(projectId, taskId);
    }

    private void requireProject(UUID projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new ResourceNotFoundException(
                    "PROJECT_NOT_FOUND",
                    "Không tìm thấy dự án với ID: " + projectId
            );
        }
    }

    private WbsItem requireTask(UUID projectId, UUID taskId) {
        WbsItem task = wbsItemRepository.findById(taskId)
                .filter(item -> projectId.equals(item.getProjectId()))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "TASK_NOT_FOUND",
                        "Không tìm thấy công việc trong dự án này."
                ));
        if (!"task".equalsIgnoreCase(task.getType())) {
            throw new IllegalArgumentException(
                    "Chỉ có thể giao đội thi công cho công việc cụ thể."
            );
        }
        return task;
    }

    private ProjectMember requireProjectCrew(UUID projectId, UUID teamMemberId) {
        ProjectMember member = projectMemberRepository
                .findByProjectIdAndUserIdAndStatus(projectId, teamMemberId, "ACTIVE")
                .orElseThrow(() -> new IllegalArgumentException(
                        "Đội thi công không còn là thành viên đang hoạt động của dự án."
                ));
        if (member.getRole() == null
                || !"WORKER".equalsIgnoreCase(member.getRole().getName())) {
            throw new IllegalArgumentException(
                    "Chỉ thành viên dự án có vai trò Đội thi công mới được phân công."
            );
        }
        return member;
    }

    public record AssignmentResult(
            WbsItem task,
            List<TeamAssignmentConflictDetector.Overlap> overlaps,
            boolean changed
    ) {
    }
}
