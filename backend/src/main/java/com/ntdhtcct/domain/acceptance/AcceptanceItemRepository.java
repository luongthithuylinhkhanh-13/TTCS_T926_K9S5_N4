package com.ntdhtcct.domain.acceptance;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcceptanceItemRepository extends JpaRepository<AcceptanceItem, UUID> {

    List<AcceptanceItem> findByAcceptanceReportId(UUID acceptanceReportId);

    List<AcceptanceItem> findByWbsItemId(UUID wbsItemId);

    List<AcceptanceItem> findByTaskId(UUID taskId);

    @Query("""
            SELECT item FROM AcceptanceItem item
            JOIN FETCH item.acceptanceReport report
            WHERE report.projectId = :projectId
              AND report.status = 'APPROVED'
              AND item.acceptedQuantity > 0
              AND item.unitPrice > 0
            ORDER BY report.periodNumber DESC, item.itemName
            """)
    List<AcceptanceItem> findApprovedByProjectId(@Param("projectId") UUID projectId);
}
