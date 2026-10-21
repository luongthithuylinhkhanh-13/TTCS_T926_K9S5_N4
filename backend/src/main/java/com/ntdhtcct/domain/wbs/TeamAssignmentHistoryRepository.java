package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TeamAssignmentHistoryRepository
        extends JpaRepository<TeamAssignmentHistory, Long> {

    List<TeamAssignmentHistory> findByProjectIdAndWbsItemIdOrderByChangedAtDesc(
            UUID projectId,
            UUID wbsItemId
    );
}
