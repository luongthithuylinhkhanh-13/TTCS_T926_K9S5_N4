package com.ntdhtcct.domain.wbs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WbsItemRepository extends JpaRepository<WbsItem, UUID> {

    List<WbsItem> findByProjectIdOrderByWbsCodeAsc(UUID projectId);

    boolean existsByProjectIdAndWbsCode(UUID projectId, String wbsCode);

    List<WbsItem> findByParentId(UUID parentId);

    @org.springframework.data.jpa.repository.Query("SELECT w FROM WbsItem w WHERE w.assigneeId = :assigneeId AND w.startDate <= :date AND w.endDate >= :date")
    List<WbsItem> findAssignedTasksByDate(@org.springframework.data.repository.query.Param("assigneeId") String assigneeId, @org.springframework.data.repository.query.Param("date") java.time.LocalDate date);
}