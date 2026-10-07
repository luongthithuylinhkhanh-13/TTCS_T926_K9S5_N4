package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ConstructionTeamService {

    private final ConstructionTeamRepository constructionTeamRepository;
    private final WbsItemRepository wbsItemRepository;
    private final TeamAssignmentHistoryRepository teamAssignmentHistoryRepository;

    public ConstructionTeamService(
            ConstructionTeamRepository constructionTeamRepository,
            WbsItemRepository wbsItemRepository,
            TeamAssignmentHistoryRepository teamAssignmentHistoryRepository
    ) {
        this.constructionTeamRepository = constructionTeamRepository;
        this.wbsItemRepository = wbsItemRepository;
        this.teamAssignmentHistoryRepository =
                teamAssignmentHistoryRepository;
    }

    @Transactional(readOnly = true)
    public List<ConstructionTeam> getTeams(Long projectId) {
        return constructionTeamRepository.findByProjectId(projectId);
    }

    @Transactional
    public ConstructionTeam createTeam(
            Long projectId,
            String code,
            String name,
            String description
    ) {
        if (projectId == null) {
            throw new IllegalArgumentException(
                    "Project không hợp lệ"
            );
        }

        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException(
                    "Mã đội thi công không được để trống"
            );
        }

        if (code.length() > 50) {
            throw new IllegalArgumentException(
                    "Mã đội thi công không được vượt quá 50 ký tự"
            );
        }

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException(
                    "Tên đội thi công không được để trống"
            );
        }

        if (name.length() > 255) {
            throw new IllegalArgumentException(
                    "Tên đội thi công không được vượt quá 255 ký tự"
            );
        }

        if (constructionTeamRepository
                .existsByProjectIdAndCode(projectId, code)) {
            throw new IllegalArgumentException(
                    "Mã đội thi công đã tồn tại trong dự án"
            );
        }

        ConstructionTeam team =
                new ConstructionTeam(
                        projectId,
                        code,
                        name,
                        description
                );

        return constructionTeamRepository.save(team);
    }

    /*
     * T-48 + T-50:
     * Gán hoặc đổi đội thi công cho WBS.
     */
    @Transactional
    public void assignTeamToWbs(
            Long projectId,
            UUID wbsId,
            UUID teamId
    ) {
        if (projectId == null) {
            throw new IllegalArgumentException(
                    "Project không hợp lệ"
            );
        }

        if (wbsId == null) {
            throw new IllegalArgumentException(
                    "Công việc không hợp lệ"
            );
        }

        if (teamId == null) {
            throw new IllegalArgumentException(
                    "Đội thi công không được để trống"
            );
        }

        WbsItem wbsItem =
                wbsItemRepository.findById(wbsId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy công việc"
                                )
                        );

        if (!projectId.equals(wbsItem.getProjectId())) {
            throw new IllegalArgumentException(
                    "Công việc không thuộc dự án"
            );
        }

        ConstructionTeam team =
                constructionTeamRepository
                        .findByProjectIdAndId(projectId, teamId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Đội thi công không tồn tại trong dự án"
                                )
                        );

        UUID oldTeamId = wbsItem.getTeamId();

        if (team.getId().equals(oldTeamId)) {
            return;
        }

        wbsItem.setTeamId(team.getId());
        wbsItemRepository.save(wbsItem);

        TeamAssignmentHistory history =
                new TeamAssignmentHistory(
                        projectId,
                        wbsId,
                        oldTeamId,
                        team.getId(),
                        null,
                        oldTeamId == null
                                ? "Gán đội thi công lần đầu"
                                : "Đổi đội thi công"
                );

        teamAssignmentHistoryRepository.save(history);
    }

    @Transactional
    public void removeTeamFromWbs(
            Long projectId,
            UUID wbsId
    ) {
        if (projectId == null) {
            throw new IllegalArgumentException(
                    "Project không hợp lệ"
            );
        }

        if (wbsId == null) {
            throw new IllegalArgumentException(
                    "Công việc không hợp lệ"
            );
        }

        WbsItem wbsItem =
                wbsItemRepository.findById(wbsId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Không tìm thấy công việc"
                                )
                        );

        if (!projectId.equals(wbsItem.getProjectId())) {
            throw new IllegalArgumentException(
                    "Công việc không thuộc dự án"
            );
        }

        UUID oldTeamId = wbsItem.getTeamId();

        if (oldTeamId == null) {
            return;
        }

        wbsItem.setTeamId(null);
        wbsItemRepository.save(wbsItem);

        TeamAssignmentHistory history =
                new TeamAssignmentHistory(
                        projectId,
                        wbsId,
                        oldTeamId,
                        null,
                        null,
                        "Bỏ đội thi công khỏi công việc"
                );

        teamAssignmentHistoryRepository.save(history);
    }

    /*
     * T-50:
     * Xem lịch sử thay đổi đội của một WBS.
     */
    @Transactional(readOnly = true)
    public List<TeamAssignmentHistory> getAssignmentHistory(
            Long projectId,
            UUID wbsId
    ) {
        return teamAssignmentHistoryRepository
                .findByProjectIdAndWbsIdOrderByChangedAtDesc(
                        projectId,
                        wbsId
                );
    }
}