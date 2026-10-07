package com.ntdhtcct.domain.wbs;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamAssignmentHistoryRepository
        extends JpaRepository<TeamAssignmentHistory, UUID> {

    List<TeamAssignmentHistory>
    findByProjectIdAndWbsIdOrderByChangedAtDesc(
            Long projectId,
            UUID wbsId
    );

    List<TeamAssignmentHistory>
    findByProjectIdOrderByChangedAtDesc(
            Long projectId
    );
}