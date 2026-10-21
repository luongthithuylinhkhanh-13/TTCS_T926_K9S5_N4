package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProjectScheduleBaselineRepository
        extends JpaRepository<ProjectScheduleBaselineItem, UUID> {

    List<ProjectScheduleBaselineItem> findByProjectIdOrderByWbsCodeAsc(UUID projectId);
}
